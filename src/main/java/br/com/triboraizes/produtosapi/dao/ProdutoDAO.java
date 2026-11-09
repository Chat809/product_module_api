package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.model.FiltroProdutos;
import br.com.triboraizes.produtosapi.model.Produto;
import br.com.triboraizes.produtosapi.model.ResumoEstoque;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Contrato de acesso a dados de {@link Produto}, independente da tecnologia de persistência. */
public interface ProdutoDAO {

    /** Colunas aceitas no {@code sort}, com o nome correspondente no banco. */
    Map<String, String> COLUNAS_ORDENAVEIS = Map.of(
            "codigo", "codigo",
            "nome", "nome",
            "categoria", "categoria",
            "preco", "preco",
            "quantidadeEmEstoque", "quantidade_em_estoque",
            "criadoEm", "criado_em",
            "atualizadoEm", "atualizado_em");

    record Pagina(List<Produto> produtos, long total) {
    }

    /** @param ordenarPor uma das chaves de {@link #COLUNAS_ORDENAVEIS} */
    Pagina listar(FiltroProdutos filtro, int pagina, int tamanho, String ordenarPor, boolean crescente);

    Optional<Produto> buscarPorId(UUID id);

    /** Grava o produto; o banco registra a entrada "Estoque inicial" se a quantidade for maior que zero. */
    Produto criar(Produto produto);

    /**
     * Grava os dados editáveis. Com {@code versaoEsperada}, só grava se o produto ainda estiver nessa versão.
     *
     * @return vazio se o produto não existir ou estiver em outra versão
     */
    Optional<Produto> atualizar(UUID id, Long versaoEsperada, Produto dados);

    /** Remove o produto e, em cascata, o histórico dele. */
    boolean excluir(UUID id);

    /** Indicadores dos produtos ativos; estoque baixo é de 1 até {@code limiteEstoqueBaixo} unidades. */
    ResumoEstoque resumir(int limiteEstoqueBaixo);

    /** Categorias em uso, em ordem alfabética e sem repetição. */
    List<String> listarCategorias();
}
