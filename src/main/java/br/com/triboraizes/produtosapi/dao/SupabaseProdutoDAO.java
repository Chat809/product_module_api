package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.CodigoDeProdutoDuplicadoException;
import br.com.triboraizes.produtosapi.model.FiltroProdutos;
import br.com.triboraizes.produtosapi.model.Produto;
import br.com.triboraizes.produtosapi.model.ResumoEstoque;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Acessa {@code public.produtos} pelo PostgREST. Versão, datas e histórico de estoque
 * ficam a cargo dos gatilhos da tabela, então cada gravação é uma única chamada.
 */
public class SupabaseProdutoDAO extends SupabaseTabela implements ProdutoDAO {

    private static final String TABELA = "/rest/v1/produtos";
    private static final int LIMITE_CATEGORIAS = 1000;
    private static final ParameterizedTypeReference<List<Produto>> LISTA = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<List<Map<String, String>>> CATEGORIAS =
            new ParameterizedTypeReference<>() {
            };

    public SupabaseProdutoDAO(RestClient supabaseRestClient, SupabaseProperties properties) {
        super(supabaseRestClient, properties);
    }

    @Override
    public Pagina listar(FiltroProdutos filtro, int pagina, int tamanho, String ordenarPor, boolean crescente) {
        String coluna = COLUNAS_ORDENAVEIS.get(ordenarPor);
        if (coluna == null) {
            throw new IllegalArgumentException("Coluna de ordenação não permitida: " + ordenarPor);
        }
        // O id desempata para a paginação ser estável; código e categoria vazios ficam no fim.
        StringBuilder uri = new StringBuilder(TABELA + "?select=*&order=" + coluna + (crescente ? ".asc" : ".desc")
                + ".nullslast,id.asc&offset={offset}&limit={limit}");
        Map<String, Object> valores = new HashMap<>();
        valores.put("offset", (long) pagina * tamanho);
        valores.put("limit", tamanho);
        if (filtro.ativo() != null) {
            uri.append("&ativo=is.").append(filtro.ativo());
        }
        String categoria = termoDeBusca(filtro.categoria());
        if (!categoria.isEmpty()) {
            uri.append("&categoria=ilike.{categoria}");
            valores.put("categoria", categoria);
        }
        String termo = termoDeBusca(filtro.busca());
        if (!termo.isEmpty()) {
            uri.append("&or={busca}");
            valores.put("busca", "(codigo.ilike.*" + termo + "*,nome.ilike.*" + termo + "*,descricao.ilike.*" + termo
                    + "*,categoria.ilike.*" + termo + "*)");
        }
        Resultado<Produto> resultado = paginado(restClient.get().uri(uri.toString(), valores), LISTA);
        return new Pagina(resultado.linhas(), resultado.total());
    }

    @Override
    public Optional<Produto> buscarPorId(UUID id) {
        return chamar(() -> primeiro(restClient.get()
                .uri(TABELA + "?select=*&id=eq.{id}", id)
                .retrieve()
                .body(LISTA)));
    }

    @Override
    public Produto criar(Produto produto) {
        Map<String, Object> dados = editaveis(produto);
        dados.put("criado_por", produto.criadoPor());
        return gravando(produto.codigo(), () -> primeiro(restClient.post()
                .uri(TABELA + "?select=*")
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .body(LISTA))).orElseThrow();
    }

    @Override
    public Optional<Produto> atualizar(UUID id, Long versaoEsperada, Produto dados) {
        Map<String, Object> valores = new HashMap<>();
        valores.put("id", id);
        String uri = TABELA + "?id=eq.{id}&select=*";
        if (versaoEsperada != null) {
            uri += "&versao=eq.{versao}";
            valores.put("versao", versaoEsperada);
        }
        String filtro = uri;
        return gravando(dados.codigo(), () -> primeiro(restClient.patch()
                .uri(filtro, valores)
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(editaveis(dados))
                .retrieve()
                .body(LISTA)));
    }

    @Override
    public boolean excluir(UUID id) {
        List<Produto> removidos = chamar(() -> restClient.delete()
                .uri(TABELA + "?id=eq.{id}&select=id", id)
                .header("Prefer", "return=representation")
                .retrieve()
                .body(LISTA));
        return removidos != null && !removidos.isEmpty();
    }

    @Override
    public ResumoEstoque resumir(int limiteEstoqueBaixo) {
        ResumoEstoque resumo = chamar(() -> restClient.post()
                .uri("/rest/v1/rpc/resumo_estoque")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("p_limite", limiteEstoqueBaixo))
                .retrieve()
                .body(ResumoEstoque.class));
        if (resumo == null) {
            return new ResumoEstoque(0, 0, BigDecimal.ZERO.setScale(2), 0, 0);
        }
        BigDecimal valor = resumo.valorTotal() == null ? BigDecimal.ZERO : resumo.valorTotal();
        return new ResumoEstoque(resumo.totalProdutos(), resumo.totalItens(), valor.setScale(2, RoundingMode.HALF_UP),
                resumo.esgotados(), resumo.estoqueBaixo());
    }

    @Override
    public List<String> listarCategorias() {
        List<Map<String, String>> linhas = chamar(() -> restClient.get()
                .uri(TABELA + "?select=categoria&categoria=not.is.null&order=categoria.asc&limit=" + LIMITE_CATEGORIAS)
                .retrieve()
                .body(CATEGORIAS));
        return linhas == null ? List.of() : linhas.stream()
                .map(linha -> linha.get("categoria"))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** O índice único do código responde 409 no PostgREST; vira um erro de negócio com o código repetido. */
    private <T> T gravando(String codigo, Supplier<T> chamada) {
        return chamar(() -> {
            try {
                return chamada.get();
            } catch (HttpClientErrorException ex) {
                if (ex.getStatusCode().value() == 409 && codigo != null) {
                    throw new CodigoDeProdutoDuplicadoException(codigo);
                }
                throw ex;
            }
        });
    }

    private static Map<String, Object> editaveis(Produto produto) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("codigo", produto.codigo());
        dados.put("nome", produto.nome());
        dados.put("descricao", produto.descricao());
        dados.put("categoria", produto.categoria());
        dados.put("unidade", produto.unidade().name());
        dados.put("preco", produto.preco());
        dados.put("custo", produto.custo());
        dados.put("quantidade_em_estoque", produto.quantidadeEmEstoque());
        dados.put("ativo", produto.ativo());
        dados.put("atualizado_por", produto.atualizadoPor());
        return dados;
    }
}
