package br.com.triboraizes.produtosapi.model;

import java.math.BigDecimal;

/** Indicadores consolidados do estoque. */
public record ResumoEstoque(
        long totalProdutos,
        long totalItens,
        BigDecimal valorTotal,
        long esgotados,
        long estoqueBaixo
) {
}
