package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.ResumoEstoque;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Indicadores do estoque para o dashboard; produtos inativos não entram")
public record ResumoEstoqueDTO(
        @Schema(description = "Quantidade de produtos ativos") long totalProdutos,
        @Schema(description = "Soma das unidades em estoque") long totalItens,
        @Schema(description = "Soma de preço × quantidade dos produtos ativos") BigDecimal valorTotal,
        @Schema(description = "Produtos com estoque zerado") long esgotados,
        @Schema(description = "Produtos com estoque entre 1 e o limite de estoque baixo") long estoqueBaixo,
        int limiteEstoqueBaixo
) {

    public static ResumoEstoqueDTO from(ResumoEstoque resumo, int limiteEstoqueBaixo) {
        return new ResumoEstoqueDTO(resumo.totalProdutos(), resumo.totalItens(), resumo.valorTotal(),
                resumo.esgotados(), resumo.estoqueBaixo(), limiteEstoqueBaixo);
    }
}
