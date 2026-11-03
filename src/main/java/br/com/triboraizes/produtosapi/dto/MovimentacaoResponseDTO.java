package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.MovimentacaoEstoque;
import br.com.triboraizes.produtosapi.model.TipoMovimentacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Movimentação de estoque registrada")
public record MovimentacaoResponseDTO(
        @Schema(example = "11cfde99-930c-4e52-80a5-ba3392bf5067")
        UUID id,
        @Schema(example = "3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10")
        UUID produtoId,
        @Schema(description = "ENTRADA, SAIDA ou AJUSTE (gerado ao editar a quantidade pelo PUT do produto)",
                example = "ENTRADA")
        TipoMovimentacao tipo,
        @Schema(example = "10")
        Integer quantidade,
        @Schema(example = "25")
        Integer saldoAnterior,
        @Schema(example = "35")
        Integer saldoApos,
        @Schema(example = "Reposição do fornecedor", nullable = true)
        String motivo,
        @Schema(description = "Data do registro (UTC)", example = "2026-11-03T05:41:52.677Z")
        Instant criadoEm
) {

    public static MovimentacaoResponseDTO from(MovimentacaoEstoque movimentacao) {
        return new MovimentacaoResponseDTO(
                movimentacao.getId(),
                movimentacao.getProduto().getId(),
                movimentacao.getTipo(),
                movimentacao.getQuantidade(),
                movimentacao.getSaldoAnterior(),
                movimentacao.getSaldoApos(),
                movimentacao.getMotivo(),
                movimentacao.getCriadoEm()
        );
    }
}
