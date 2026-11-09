package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.Conta;
import br.com.triboraizes.produtosapi.model.FormaPagamento;
import br.com.triboraizes.produtosapi.model.StatusConta;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Conta a pagar ou a receber")
public record ContaDTO(
        UUID id,
        @Schema(example = "Aluguel do depósito") String descricao,
        @Schema(description = "Favorecido (a pagar) ou pagador (a receber)", example = "Imobiliária Centro") String pessoa,
        String documento,
        String categoria,
        @Schema(example = "2500.00") BigDecimal valor,
        @Schema(example = "6.50") BigDecimal percentualImposto,
        @Schema(description = "round(valor x percentual / 100, 2)", example = "162.50") BigDecimal valorImposto,
        @Schema(description = "valor - imposto: o que de fato sai ou entra", example = "2337.50") BigDecimal valorLiquido,
        @Schema(example = "2026-11-10") LocalDate vencimento,
        @Schema(description = "Data do pagamento ou recebimento", nullable = true) LocalDate dataBaixa,
        @Schema(example = "PENDENTE") StatusConta status,
        @Schema(description = "Pendente com vencimento antes de hoje") boolean vencida,
        @Schema(example = "PIX") FormaPagamento formaPagamento,
        String chavePix,
        String observacoes,
        UUID criadoPor,
        Instant criadoEm,
        Instant atualizadoEm,
        @Schema(description = "Fornecedor (a pagar) ou cliente (a receber) cadastrado", nullable = true) UUID vinculoId,
        @Schema(nullable = true) UUID centroCustoId
) {

    public static ContaDTO from(Conta conta, LocalDate hoje) {
        boolean vencida = conta.status() == StatusConta.PENDENTE && conta.vencimento() != null
                && conta.vencimento().isBefore(hoje);
        return new ContaDTO(conta.id(), conta.descricao(), conta.pessoa(), conta.documento(), conta.categoria(),
                conta.valor(), conta.percentualImposto(), conta.valorImposto(), conta.valorLiquido(),
                conta.vencimento(), conta.dataBaixa(), conta.status(), vencida, conta.formaPagamento(),
                conta.chavePix(), conta.observacoes(), conta.criadoPor(),
                conta.criadoEm() == null ? null : conta.criadoEm().toInstant(),
                conta.atualizadoEm() == null ? null : conta.atualizadoEm().toInstant(),
                conta.vinculoId(), conta.centroCustoId());
    }
}
