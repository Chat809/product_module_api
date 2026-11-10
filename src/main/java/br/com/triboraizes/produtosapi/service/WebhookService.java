package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.CadastroDAO;
import br.com.triboraizes.produtosapi.dao.CadastroDAO.Consulta;
import br.com.triboraizes.produtosapi.dto.LeadRecebidoDTO;
import br.com.triboraizes.produtosapi.dto.LeadRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.WebhookEntradaDTO;
import br.com.triboraizes.produtosapi.dto.WebhookEntradaRequestDTO;
import br.com.triboraizes.produtosapi.dto.WebhookSaidaDTO;
import br.com.triboraizes.produtosapi.dto.WebhookSaidaRequestDTO;
import br.com.triboraizes.produtosapi.evento.EventoDeNegocio;
import br.com.triboraizes.produtosapi.exception.RegistroNaoEncontradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.Lead;
import br.com.triboraizes.produtosapi.model.WebhookEntrada;
import br.com.triboraizes.produtosapi.model.WebhookEntrega;
import br.com.triboraizes.produtosapi.model.WebhookSaida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cadastro dos webhooks de saída e de entrada, log de entregas, envio de teste e recebimento de leads. */
@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final CadastroDAO<WebhookSaida> saidas;
    private final CadastroDAO<WebhookEntrega> entregas;
    private final CadastroDAO<WebhookEntrada> entradas;
    private final WebhookDisparador disparador;
    private final LeadService leads;

    public WebhookService(DAOFactory fabrica, WebhookDisparador disparador, LeadService leads) {
        this.saidas = fabrica.criarWebhookSaidaDAO();
        this.entregas = fabrica.criarWebhookEntregaDAO();
        this.entradas = fabrica.criarWebhookEntradaDAO();
        this.disparador = disparador;
        this.leads = leads;
    }

    public List<WebhookSaidaDTO> saidas() {
        return saidas.todos(Map.of(), "criado_em.asc").stream().map(w -> WebhookSaidaDTO.from(w, false)).toList();
    }

    public WebhookSaidaDTO criarSaida(WebhookSaidaRequestDTO request, UUID usuario) {
        Map<String, Object> dados = dadosDaSaida(request);
        dados.put("segredo", novoSegredo());
        dados.put("criado_por", usuario);
        WebhookSaida criado = saidas.criar(dados);
        log.info("Webhook de saída cadastrado: id={}, por={}", criado.id(), usuario);
        return WebhookSaidaDTO.from(criado, true);
    }

    public WebhookSaidaDTO atualizarSaida(UUID id, WebhookSaidaRequestDTO request) {
        return WebhookSaidaDTO.from(saidas.atualizar(id, dadosDaSaida(request)).orElseThrow(() -> saidaNaoEncontrada(id)),
                false);
    }

    public WebhookSaidaDTO regenerarSegredo(UUID id, UUID usuario) {
        WebhookSaida atualizado = saidas.atualizar(id, Map.of("segredo", novoSegredo()))
                .orElseThrow(() -> saidaNaoEncontrada(id));
        log.info("Segredo do webhook {} regenerado por {}", id, usuario);
        return WebhookSaidaDTO.from(atualizado, true);
    }

    public void excluirSaida(UUID id) {
        if (!saidas.excluir(id)) {
            throw saidaNaoEncontrada(id);
        }
    }

    /** Envia um evento {@code teste} na hora (sem esperar o fluxo assíncrono) e devolve o resultado. */
    public WebhookEntrega testar(UUID id) {
        WebhookSaida webhook = saidas.buscar(id).orElseThrow(() -> saidaNaoEncontrada(id));
        return disparador.entregar(webhook, "teste",
                Map.of("mensagem", "Envio de teste do painel Tribo Raízes", "webhook", webhook.nome()),
                OffsetDateTime.now());
    }

    public PaginaDTO<WebhookEntrega> entregas(UUID webhookId, Boolean sucesso, int pagina, int tamanho) {
        Entradas.validarPagina(pagina, tamanho);
        Consulta consulta = Consulta.de("criado_em.desc,id.asc", pagina, tamanho);
        if (webhookId != null) {
            consulta.onde("webhook_id", "eq." + webhookId);
        }
        if (sucesso != null) {
            consulta.onde("sucesso", "is." + sucesso);
        }
        CadastroDAO.Pagina<WebhookEntrega> resultado = entregas.listar(consulta);
        return PaginaDTO.of(resultado.itens(), e -> e, pagina, tamanho, resultado.total());
    }

    public List<WebhookEntradaDTO> entradas() {
        return entradas.todos(Map.of(), "criado_em.asc").stream().map(e -> WebhookEntradaDTO.from(e, false)).toList();
    }

    public WebhookEntradaDTO criarEntrada(WebhookEntradaRequestDTO request, UUID usuario) {
        Map<String, Object> dados = dadosDaEntrada(request);
        dados.put("token", novoToken());
        dados.put("criado_por", usuario);
        WebhookEntrada criada = entradas.criar(dados);
        log.info("Webhook de entrada cadastrado: id={}, por={}", criada.id(), usuario);
        return WebhookEntradaDTO.from(criada, true);
    }

    public WebhookEntradaDTO atualizarEntrada(UUID id, WebhookEntradaRequestDTO request) {
        return WebhookEntradaDTO.from(entradas.atualizar(id, dadosDaEntrada(request))
                .orElseThrow(() -> entradaNaoEncontrada(id)), false);
    }

    public WebhookEntradaDTO regenerarToken(UUID id, UUID usuario) {
        WebhookEntrada atualizada = entradas.atualizar(id, Map.of("token", novoToken()))
                .orElseThrow(() -> entradaNaoEncontrada(id));
        log.info("Token do webhook de entrada {} regenerado por {}", id, usuario);
        return WebhookEntradaDTO.from(atualizada, true);
    }

    public void excluirEntrada(UUID id) {
        if (!entradas.excluir(id)) {
            throw entradaNaoEncontrada(id);
        }
    }

    /** Cria o lead recebido por um webhook de entrada ativo. Token desconhecido ou inativo dá 404, sem detalhes. */
    public Lead receberLead(String token, LeadRecebidoDTO recebido) {
        WebhookEntrada entrada = token == null || !token.matches("[A-Za-z0-9_-]{32,128}") ? null
                : entradas.todos(Map.of("token", "eq." + token, "ativo", "is.true"), "id.asc").stream().findFirst()
                .orElse(null);
        if (entrada == null) {
            throw new RegistroNaoEncontradoException("Endereço não encontrado", "Endereço de integração não encontrado");
        }
        LeadRequestDTO lead = new LeadRequestDTO(recebido.nome(), recebido.empresa(), recebido.email(),
                recebido.telefone(), entrada.origemId(), null, recebido.valorEstimado(), null, null, null,
                recebido.mensagem());
        Lead criado = leads.criarPorIntegracao(lead, entrada.nome());
        Map<String, Object> uso = new HashMap<>();
        uso.put("total_recebidos", entrada.totalRecebidos() + 1);
        uso.put("ultimo_uso_em", OffsetDateTime.now().toString());
        entradas.atualizar(entrada.id(), uso);
        return criado;
    }

    private static Map<String, Object> dadosDaSaida(WebhookSaidaRequestDTO request) {
        List<String> eventos = List.copyOf(new LinkedHashSet<>(request.eventos()));
        List<String> desconhecidos = eventos.stream().filter(e -> !EventoDeNegocio.TIPOS.contains(e)).toList();
        if (!desconhecidos.isEmpty()) {
            throw new RequisicaoInvalidaException("Eventos desconhecidos: " + String.join(", ", desconhecidos));
        }
        String url = request.url().strip();
        WebhookDisparador.exigirEnderecoPublico(url);
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", request.nome().strip());
        dados.put("url", url);
        dados.put("eventos", eventos);
        dados.put("ativo", request.ativo() == null || request.ativo());
        return dados;
    }

    private static Map<String, Object> dadosDaEntrada(WebhookEntradaRequestDTO request) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", request.nome().strip());
        dados.put("origem_id", request.origemId());
        dados.put("ativo", request.ativo() == null || request.ativo());
        return dados;
    }

    private static String novoSegredo() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String novoToken() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static RegistroNaoEncontradoException saidaNaoEncontrada(UUID id) {
        return new RegistroNaoEncontradoException("Webhook não encontrado", "Webhook " + id + " não encontrado");
    }

    private static RegistroNaoEncontradoException entradaNaoEncontrada(UUID id) {
        return new RegistroNaoEncontradoException("Webhook de entrada não encontrado",
                "Webhook de entrada " + id + " não encontrado");
    }
}
