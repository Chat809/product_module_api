package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Registro do pagamento ou recebimento")
public record BaixaRequestDTO(
        @Schema(description = "Data do pagamento ou recebimento; padrão hoje. Não pode ser futura.",
                example = "2026-11-03", nullable = true)
        LocalDate data
) {
}
