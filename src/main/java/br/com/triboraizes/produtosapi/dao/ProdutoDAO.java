package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.model.Produto;
import br.com.triboraizes.produtosapi.model.ResumoEstoque;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Contrato de acesso a dados de {@link Produto}, independente da tecnologia de persistência.
 * As operações devem ser executadas dentro de uma transação aberta pela camada de serviço.
 */
public interface ProdutoDAO {

    Set<String> COLUNAS_ORDENAVEIS = Set.of("nome", "preco", "quantidadeEmEstoque", "criadoEm", "atualizadoEm");

    /** Grava o produto e sincroniza com o banco, preenchendo id, datas e versão. */
    Produto salvar(Produto produto);

    Optional<Produto> buscarPorId(UUID id);

    /**
     * Lista uma página de produtos.
     *
     * @param busca     trecho procurado no nome ou na descrição, sem diferenciar maiúsculas; {@code null} lista todos
     * @param ordenarPor uma das {@link #COLUNAS_ORDENAVEIS}
     */
    List<Produto> listar(String busca, int pagina, int tamanho, String ordenarPor, boolean crescente);

    long contar(String busca);

    /** Calcula os indicadores do estoque; estoque baixo é de 1 até {@code limiteEstoqueBaixo} unidades. */
    ResumoEstoque resumir(int limiteEstoqueBaixo);

    /** Grava as alterações e sincroniza com o banco, atualizando a data e a versão. */
    Produto atualizar(Produto produto);

    void remover(Produto produto);
}
