package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.model.MovimentacaoEstoque;
import br.com.triboraizes.produtosapi.model.Produto;

import java.util.List;

/**
 * Contrato de acesso a dados de {@link MovimentacaoEstoque}.
 * As operações devem ser executadas dentro de uma transação aberta pela camada de serviço.
 */
public interface MovimentacaoDAO {

    /** Grava a movimentação e sincroniza com o banco, preenchendo id e data. */
    MovimentacaoEstoque salvar(MovimentacaoEstoque movimentacao);

    /** Lista uma página do histórico do produto, da mais recente para a mais antiga. */
    List<MovimentacaoEstoque> listarPorProduto(Produto produto, int pagina, int tamanho);

    long contarPorProduto(Produto produto);

    void removerPorProduto(Produto produto);
}
