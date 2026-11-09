package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.exception.EstoqueInsuficienteException;
import br.com.triboraizes.produtosapi.model.MovimentacaoEstoque;
import br.com.triboraizes.produtosapi.model.TipoMovimentacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Contrato de acesso a dados de {@link MovimentacaoEstoque}. */
public interface MovimentacaoDAO {

    record Pagina(List<MovimentacaoEstoque> movimentacoes, long total) {
    }

    /**
     * Aplica uma entrada ou saída ao estoque e grava o histórico na mesma transação do banco.
     *
     * @return vazio se o produto não existir
     * @throws EstoqueInsuficienteException se a saída for maior que o saldo no momento da gravação
     */
    Optional<MovimentacaoEstoque> registrar(UUID produtoId, TipoMovimentacao tipo, int quantidade, String motivo,
                                            UUID usuario);

    /** Uma página do histórico do produto, da mais recente para a mais antiga. */
    Pagina listarPorProduto(UUID produtoId, int pagina, int tamanho);
}
