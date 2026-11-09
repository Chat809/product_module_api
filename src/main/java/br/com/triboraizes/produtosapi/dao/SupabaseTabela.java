package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.client.FalhaDoSupabase;
import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.ServicoDeDadosIndisponivelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Base dos DAOs que usam a API REST do Supabase (PostgREST) com a chave secreta.
 * As tabelas têm RLS sem políticas, então nada é lido ou gravado sem passar pela API.
 */
abstract class SupabaseTabela {

    private static final Logger log = LoggerFactory.getLogger(SupabaseTabela.class);

    protected final RestClient restClient;
    private final SupabaseProperties properties;

    protected SupabaseTabela(RestClient restClient, SupabaseProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    protected <T> T chamar(Supplier<T> chamada) {
        if (!properties.configurado()) {
            throw new ServicoDeDadosIndisponivelException(
                    "Serviço de dados não configurado (defina SUPABASE_SECRET_KEY)", null);
        }
        try {
            return chamada.get();
        } catch (RestClientException ex) {
            log.error("Falha ao acessar o banco no Supabase ({}): {}", getClass().getSimpleName(),
                    FalhaDoSupabase.descrever(ex));
            throw new ServicoDeDadosIndisponivelException("Serviço de dados indisponível. Tente novamente.", ex);
        }
    }

    protected record Resultado<T>(List<T> linhas, long total) {
    }

    /** Executa a consulta pedindo a contagem: o total vem no {@code Content-Range} (por exemplo {@code 0-19/57}). */
    protected <T> Resultado<T> paginado(RestClient.RequestHeadersSpec<?> consulta, ParameterizedTypeReference<List<T>> tipo) {
        return chamar(() -> {
            try {
                ResponseEntity<List<T>> resposta = consulta.header("Prefer", "count=exact").retrieve().toEntity(tipo);
                List<T> linhas = resposta.getBody() == null ? List.of() : resposta.getBody();
                return new Resultado<>(linhas, total(resposta.getHeaders().getFirst("Content-Range")));
            } catch (HttpClientErrorException ex) {
                // Página além do fim: o PostgREST responde 416 com o total em "*/57".
                if (ex.getStatusCode().value() == 416 && ex.getResponseHeaders() != null) {
                    return new Resultado<>(List.of(), total(ex.getResponseHeaders().getFirst("Content-Range")));
                }
                throw ex;
            }
        });
    }

    private static long total(String intervalo) {
        if (intervalo == null || !intervalo.contains("/") || intervalo.endsWith("*")) {
            return 0L;
        }
        return Long.parseLong(intervalo.substring(intervalo.indexOf('/') + 1));
    }

    protected static <T> Optional<T> primeiro(List<T> linhas) {
        return linhas == null || linhas.isEmpty() ? Optional.empty() : Optional.of(linhas.getFirst());
    }

    /**
     * Remove os caracteres com significado na sintaxe de filtros do PostgREST ({@code , ( ) * " \ %}),
     * para que o texto digitado não altere o filtro.
     */
    protected static String termoDeBusca(String busca) {
        return busca == null ? "" : busca.replaceAll("[,()*\"\\\\%]", " ").trim();
    }
}
