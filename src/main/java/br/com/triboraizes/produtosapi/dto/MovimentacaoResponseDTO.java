package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.MovimentacaoEstoque;
import br.com.triboraizes.produtosapi.model.TipoMovimentacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Movimentação de estoque registrada")
public record MovimentacaoResponseDTO(
        UUID id,
        UUID produtoId,
        TipoMovimentacao tipo,
        Integer quantidade,
        Integer saldoAnterior,
        Integer saldoApos,
        String motivo,
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
