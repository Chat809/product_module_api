package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.client.FalhaDoSupabase;
import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.ServicoDeUsuariosIndisponivelException;
import br.com.triboraizes.produtosapi.model.Papel;
import br.com.triboraizes.produtosapi.model.Perfil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Acessa a tabela {@code public.perfis} pela API REST do Supabase (PostgREST).
 * A tabela tem RLS sem políticas: só a chave secreta do backend consegue ler e escrever.
 */
@Repository
public class SupabasePerfilDAO implements PerfilDAO {

    private static final Logger log = LoggerFactory.getLogger(SupabasePerfilDAO.class);

    private static final String TABELA = "/rest/v1/perfis";
    private static final String COLUNAS = "id,nome,email,papel,acessos,criado_em,telefone,funcao,bio,avatar_url";
    private static final ParameterizedTypeReference<List<Perfil>> LISTA = new ParameterizedTypeReference<>() {
    };

    private final RestClient restClient;
    private final SupabaseProperties properties;

    public SupabasePerfilDAO(RestClient supabaseRestClient, SupabaseProperties properties) {
        this.restClient = supabaseRestClient;
        this.properties = properties;
    }

    @Override
    public Optional<Perfil> buscarPorId(UUID id) {
        return chamar(() -> primeiro(restClient.get()
                .uri(TABELA + "?select=" + COLUNAS + "&id=eq.{id}", id)
                .retrieve()
                .body(LISTA)));
    }

    @Override
    public List<Perfil> listar(int pagina, int tamanho) {
        return chamar(() -> restClient.get()
                .uri(TABELA + "?select=" + COLUNAS + "&order=nome.asc&offset={offset}&limit={limit}",
                        (long) pagina * tamanho, tamanho)
                .retrieve()
                .body(LISTA));
    }

    /** O PostgREST devolve o total no cabeçalho {@code Content-Range}, por exemplo {@code 0-9/15} ou {@code *}{@code /0}. */
    @Override
    public long contar() {
        return chamar(() -> {
            String intervalo = restClient.head()
                    .uri(TABELA + "?select=id")
                    .header("Prefer", "count=exact")
                    .retrieve()
                    .toBodilessEntity()
                    .getHeaders()
                    .getFirst("Content-Range");
            if (intervalo == null || !intervalo.contains("/")) {
                return 0L;
            }
            return Long.parseLong(intervalo.substring(intervalo.indexOf('/') + 1));
        });
    }

    @Override
    public Optional<Perfil> atualizar(UUID id, String nome, String email, Papel papel, List<String> acessos) {
        return gravar(id, Map.of("nome", nome, "email", email, "papel", papel.name(), "acessos", acessos));
    }

    @Override
    public Optional<Perfil> atualizarDadosProprios(UUID id, String nome, String telefone, String funcao, String bio) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", nome);
        dados.put("telefone", telefone);
        dados.put("funcao", funcao);
        dados.put("bio", bio);
        return gravar(id, dados);
    }

    @Override
    public Optional<Perfil> atualizarAvatar(UUID id, String avatarUrl) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("avatar_url", avatarUrl);
        return gravar(id, dados);
    }

    @Override
    public List<Perfil> listarOpcoes() {
        return chamar(() -> restClient.get()
                .uri(TABELA + "?select=id,nome,avatar_url&order=nome.asc&limit=1000")
                .retrieve()
                .body(LISTA));
    }

    private Optional<Perfil> gravar(UUID id, Map<String, Object> dados) {
        return chamar(() -> primeiro(restClient.patch()
                .uri(TABELA + "?id=eq.{id}&select=" + COLUNAS, id)
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .body(LISTA)));
    }

    private static Optional<Perfil> primeiro(List<Perfil> perfis) {
        return perfis == null || perfis.isEmpty() ? Optional.empty() : Optional.of(perfis.getFirst());
    }

    private <T> T chamar(Supplier<T> chamada) {
        if (!properties.configurado()) {
            throw new ServicoDeUsuariosIndisponivelException(
                    "Serviço de usuários não configurado (defina SUPABASE_SECRET_KEY)", null);
        }
        try {
            return chamada.get();
        } catch (RestClientException ex) {
            log.error("Falha ao acessar os perfis no Supabase: {}", FalhaDoSupabase.descrever(ex));
            throw new ServicoDeUsuariosIndisponivelException("Serviço de usuários indisponível. Tente novamente.", ex);
        }
    }
}
