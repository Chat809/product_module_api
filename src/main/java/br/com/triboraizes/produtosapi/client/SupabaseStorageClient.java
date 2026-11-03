package br.com.triboraizes.produtosapi.client;

import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.ServicoDeDadosIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Envio e remoção de arquivos no Supabase Storage com a chave secreta. Os buckets não têm políticas de escrita,
 * então só o backend grava; a leitura de buckets públicos é feita direto pela URL pública.
 */
@Component
public class SupabaseStorageClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageClient.class);

    private final RestClient restClient;
    private final SupabaseProperties properties;

    public SupabaseStorageClient(RestClient supabaseRestClient, SupabaseProperties properties) {
        this.restClient = supabaseRestClient;
        this.properties = properties;
    }

    /** Grava {@code pasta/arquivo} no bucket e devolve a URL pública. */
    public String enviar(String bucket, String pasta, String arquivo, byte[] conteudo, String tipo) {
        if (!properties.configurado()) {
            throw new ServicoDeDadosIndisponivelException(
                    "Serviço de arquivos não configurado (defina SUPABASE_SECRET_KEY)", null);
        }
        try {
            restClient.post()
                    .uri("/storage/v1/object/{bucket}/{pasta}/{arquivo}", bucket, pasta, arquivo)
                    .contentType(MediaType.parseMediaType(tipo))
                    .header("x-upsert", "false")
                    .body(conteudo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Falha ao enviar arquivo ao Supabase Storage (bucket={}): {}", bucket, FalhaDoSupabase.descrever(ex));
            throw new ServicoDeDadosIndisponivelException("Não foi possível enviar a imagem. Tente novamente.", ex);
        }
        return prefixoPublico(bucket) + pasta + "/" + arquivo;
    }

    /** Remove o arquivo; uma falha aqui só deixa um arquivo órfão, então é registrada e não interrompe a operação. */
    public void remover(String bucket, String caminho) {
        try {
            restClient.method(HttpMethod.DELETE)
                    .uri("/storage/v1/object/{bucket}", bucket)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("prefixes", List.of(caminho)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Não foi possível remover um arquivo antigo do bucket {}: {}", bucket, FalhaDoSupabase.descrever(ex));
        }
    }

    /** Caminho dentro do bucket a partir da URL pública, ou vazio se a URL não for deste bucket. */
    public Optional<String> caminhoDe(String bucket, String urlPublica) {
        String prefixo = prefixoPublico(bucket);
        if (urlPublica == null || !urlPublica.startsWith(prefixo)) {
            return Optional.empty();
        }
        return Optional.of(urlPublica.substring(prefixo.length()));
    }

    private String prefixoPublico(String bucket) {
        return properties.url() + "/storage/v1/object/public/" + bucket + "/";
    }
}
