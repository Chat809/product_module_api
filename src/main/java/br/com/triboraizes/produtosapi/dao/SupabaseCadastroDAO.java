package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.ConflitoDeDadosException;
import br.com.triboraizes.produtosapi.exception.OperacaoNaoPermitidaException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link CadastroDAO} sobre uma tabela do PostgREST. Os erros de restrição do Postgres viram exceções de negócio:
 * valor único repetido (23505) e exclusão de registro ainda referenciado (23503) dão 409, violação de
 * {@code check} (23514) dá 400 e erros levantados pelas funções do banco ({@code P0001}) dão 422 com a mensagem delas.
 */
public class SupabaseCadastroDAO<T> extends SupabaseTabela implements CadastroDAO<T> {

    private static final Pattern CODIGO = Pattern.compile("\"code\"\\s*:\\s*\"([0-9A-Z]{5})\"");
    private static final Pattern MENSAGEM = Pattern.compile("\"message\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final String tabela;
    private final String colunas;
    private final String nome;
    private final ParameterizedTypeReference<List<T>> lista;

    /**
     * @param tabela  tabela no schema public
     * @param colunas o {@code select} do PostgREST, que pode embutir tabelas relacionadas
     * @param nome    o cadastro no singular, para as mensagens ("o fornecedor")
     * @param tipo    a classe das linhas
     */
    public SupabaseCadastroDAO(RestClient restClient, SupabaseProperties properties, String tabela, String colunas,
                               String nome, Class<T> tipo) {
        super(restClient, properties);
        this.tabela = "/rest/v1/" + tabela;
        this.colunas = colunas;
        this.nome = nome;
        this.lista = ParameterizedTypeReference.forType(ResolvableType.forClassWithGenerics(List.class, tipo).getType());
    }

    @Override
    public Pagina<T> listar(Consulta consulta) {
        Map<String, Object> valores = new HashMap<>();
        StringBuilder uri = new StringBuilder(tabela + "?select={colunas}&order={ordem}&offset={offset}&limit={limit}");
        valores.put("colunas", colunas);
        valores.put("ordem", consulta.ordem());
        valores.put("offset", (long) consulta.pagina() * consulta.tamanho());
        valores.put("limit", consulta.tamanho());
        acrescentarFiltros(uri, valores, consulta.filtros());
        Resultado<T> resultado = paginado(restClient.get().uri(uri.toString(), valores), lista);
        return new Pagina<>(resultado.linhas(), resultado.total());
    }

    @Override
    public List<T> todos(Map<String, String> filtros, String ordem) {
        Map<String, Object> valores = new HashMap<>();
        StringBuilder uri = new StringBuilder(tabela + "?select={colunas}&order={ordem}&limit=1000");
        valores.put("colunas", colunas);
        valores.put("ordem", ordem);
        acrescentarFiltros(uri, valores, filtros);
        List<T> linhas = chamar(() -> restClient.get().uri(uri.toString(), valores).retrieve().body(lista));
        return linhas == null ? List.of() : linhas;
    }

    @Override
    public Optional<T> buscar(UUID id) {
        return chamar(() -> primeiro(restClient.get()
                .uri(tabela + "?select={colunas}&id=eq.{id}", colunas, id)
                .retrieve()
                .body(lista)));
    }

    @Override
    public T criar(Map<String, Object> dados) {
        return gravando(() -> primeiro(restClient.post()
                .uri(tabela + "?select={colunas}", colunas)
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .body(lista))).orElseThrow();
    }

    @Override
    public Optional<T> atualizar(UUID id, Map<String, Object> dados) {
        return gravando(() -> primeiro(restClient.patch()
                .uri(tabela + "?select={colunas}&id=eq.{id}", colunas, id)
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .body(lista)));
    }

    @Override
    public boolean excluir(UUID id) {
        List<Map<String, Object>> removidas = gravando(() -> restClient.delete()
                .uri(tabela + "?id=eq.{id}&select=id", id)
                .header("Prefer", "return=representation")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                }));
        return removidas != null && !removidas.isEmpty();
    }

    @Override
    public List<T> rpc(String funcao, Map<String, Object> parametros) {
        List<T> linhas = gravando(() -> restClient.post()
                .uri("/rest/v1/rpc/" + funcao + "?select={colunas}", colunas)
                .contentType(MediaType.APPLICATION_JSON)
                .body(parametros)
                .retrieve()
                .body(lista));
        return linhas == null ? List.of() : linhas;
    }

    private static void acrescentarFiltros(StringBuilder uri, Map<String, Object> valores, Map<String, String> filtros) {
        int indice = 0;
        for (Map.Entry<String, String> filtro : filtros.entrySet()) {
            String variavel = "f" + indice++;
            uri.append('&').append(filtro.getKey()).append("={").append(variavel).append('}');
            valores.put(variavel, filtro.getValue());
        }
    }

    private <R> R gravando(Supplier<R> chamada) {
        try {
            return chamar(chamada);
        } catch (RuntimeException ex) {
            HttpClientErrorException erro = ex instanceof HttpClientErrorException http ? http
                    : ex.getCause() instanceof HttpClientErrorException http ? http : null;
            if (erro == null) {
                throw ex;
            }
            String corpo = erro.getResponseBodyAsString();
            String codigo = extrair(CODIGO, corpo);
            switch (codigo == null ? "" : codigo) {
                case "23505" -> throw new ConflitoDeDadosException("Registro duplicado",
                        "Já existe " + nome + " com estes dados (" + restricao(corpo) + ")");
                case "23503" -> throw new ConflitoDeDadosException("Registro em uso",
                        "Não é possível concluir: " + nome + " está ligado a outros registros. Inative em vez de excluir.");
                case "23514", "22P02", "23502" -> throw new RequisicaoInvalidaException(
                        "Dados inválidos para " + nome + " (" + restricao(corpo) + ")");
                case "P0001" -> throw new OperacaoNaoPermitidaException(
                        Optional.ofNullable(extrair(MENSAGEM, corpo)).orElse("Operação não permitida"));
                default -> throw ex;
            }
        }
    }

    private static String restricao(String corpo) {
        String mensagem = extrair(MENSAGEM, corpo);
        return mensagem == null ? "restrição do banco" : mensagem;
    }

    private static String extrair(Pattern padrao, String corpo) {
        if (corpo == null) {
            return null;
        }
        Matcher matcher = padrao.matcher(corpo);
        return matcher.find() ? matcher.group(1) : null;
    }
}
