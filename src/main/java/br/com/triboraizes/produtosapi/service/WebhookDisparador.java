package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.CadastroDAO;
import br.com.triboraizes.produtosapi.evento.EventoDeNegocio;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.WebhookEntrega;
import br.com.triboraizes.produtosapi.model.WebhookSaida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entrega os eventos de negócio aos webhooks de saída. Cada envio leva o cabeçalho {@code X-Tribo-Assinatura}
 * ({@code sha256=} + HMAC-SHA256 do texto {@code <timestamp>.<corpo>} com o segredo do webhook) e
 * {@code X-Tribo-Timestamp}, para o destino conferir a origem e recusar reenvios antigos. Falhas são repetidas
 * até 3 vezes com espera crescente, e o resultado final fica no log de entregas.
 */
@Service
public class WebhookDisparador {

    private static final Logger log = LoggerFactory.getLogger(WebhookDisparador.class);
    private static final int TENTATIVAS = 3;
    private static final long[] ESPERAS_MS = {2_000, 8_000};
    private static final int LIMITE_DO_ERRO = 500;

    private final CadastroDAO<WebhookSaida> webhooks;
    private final CadastroDAO<WebhookEntrega> entregas;
    private final JsonMapper json;
    private final RestClient http;

    public WebhookDisparador(DAOFactory fabrica, JsonMapper json) {
        this.webhooks = fabrica.criarWebhookSaidaDAO();
        this.entregas = fabrica.criarWebhookEntregaDAO();
        this.json = json;
        JdkClientHttpRequestFactory fabricaHttp = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
        fabricaHttp.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder().requestFactory(fabricaHttp).build();
    }

    @Async
    @EventListener
    public void aoOcorrer(EventoDeNegocio evento) {
        List<WebhookSaida> destinos;
        try {
            destinos = webhooks.todos(Map.of("ativo", "is.true", "eventos", "cs.{" + evento.tipo() + "}"), "criado_em.asc");
        } catch (RuntimeException ex) {
            log.error("Não foi possível consultar os webhooks do evento {}: {}", evento.tipo(), ex.getMessage());
            return;
        }
        for (WebhookSaida destino : destinos) {
            entregar(destino, evento.tipo(), evento.dados(), evento.ocorridoEm());
        }
    }

    /** Envia (com novas tentativas) e grava o resultado. Também usado pelo botão "Enviar teste". */
    public WebhookEntrega entregar(WebhookSaida destino, String tipo, Object dados, OffsetDateTime ocorridoEm) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", UUID.randomUUID().toString());
        envelope.put("evento", tipo);
        envelope.put("ocorridoEm", ocorridoEm.toString());
        envelope.put("dados", dados);
        String corpo = json.writeValueAsString(envelope);

        Integer status = null;
        String erro = null;
        int tentativa = 0;
        long inicio = System.nanoTime();
        while (tentativa < TENTATIVAS) {
            tentativa++;
            try {
                exigirEnderecoPublico(destino.url());
                status = enviar(destino, tipo, envelope.get("id").toString(), corpo);
                erro = status >= 200 && status < 300 ? null : "Resposta HTTP " + status;
            } catch (RequisicaoInvalidaException ex) {
                erro = ex.getMessage();
                break;
            } catch (RuntimeException ex) {
                status = null;
                erro = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            }
            if (erro == null || tentativa == TENTATIVAS) {
                break;
            }
            esperar(ESPERAS_MS[tentativa - 1]);
        }
        boolean sucesso = erro == null;
        if (!sucesso) {
            log.warn("Webhook {} falhou no evento {} após {} tentativa(s): {}", destino.id(), tipo, tentativa, erro);
        }
        Map<String, Object> registro = new HashMap<>();
        registro.put("webhook_id", destino.id());
        registro.put("evento", tipo);
        registro.put("payload", envelope);
        registro.put("status_http", status);
        registro.put("sucesso", sucesso);
        registro.put("tentativas", tentativa);
        registro.put("erro", erro == null ? null : erro.substring(0, Math.min(erro.length(), LIMITE_DO_ERRO)));
        registro.put("duracao_ms", (int) Duration.ofNanos(System.nanoTime() - inicio).toMillis());
        return entregas.criar(registro);
    }

    /** Recusa destinos na rede interna (localhost, faixas privadas, link-local), mesmo que o nome pareça público. */
    static void exigirEnderecoPublico(String url) {
        String host = URI.create(url).getHost();
        if (host == null) {
            throw new RequisicaoInvalidaException("URL do webhook inválida");
        }
        try {
            for (InetAddress endereco : InetAddress.getAllByName(host)) {
                boolean unicoLocal = endereco instanceof Inet6Address && (endereco.getAddress()[0] & 0xfe) == 0xfc;
                if (endereco.isLoopbackAddress() || endereco.isSiteLocalAddress() || endereco.isLinkLocalAddress()
                        || endereco.isAnyLocalAddress() || endereco.isMulticastAddress() || unicoLocal) {
                    throw new RequisicaoInvalidaException("O webhook aponta para um endereço de rede interna");
                }
            }
        } catch (UnknownHostException ex) {
            throw new RequisicaoInvalidaException("Não foi possível encontrar o servidor " + host);
        }
    }

    private int enviar(WebhookSaida destino, String tipo, String idDaEntrega, String corpo) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        return http.post()
                .uri(URI.create(destino.url()))
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "Tribo-Raizes-Webhooks/1.0")
                .header("X-Tribo-Evento", tipo)
                .header("X-Tribo-Entrega", idDaEntrega)
                .header("X-Tribo-Timestamp", timestamp)
                .header("X-Tribo-Assinatura", "sha256=" + assinar(destino.segredo(), timestamp + "." + corpo))
                .body(corpo)
                .exchange((requisicao, resposta) -> resposta.getStatusCode().value());
    }

    static String assinar(String segredo, String conteudo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(conteudo.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 indisponível", ex);
        }
    }

    private static void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
