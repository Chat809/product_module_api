package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.FormaPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Lançamento de conta a pagar ou a receber")
public record ContaRequestDTO(

        @Schema(example = "Aluguel do depósito")
        @NotBlank(message = "A descrição é obrigatória")
        @Size(max = 150, message = "A descrição deve ter no máximo 150 caracteres")
        String descricao,

        @Schema(description = "A pagar: a quem se paga (favorecido). A receber: quem deve (pagador).",
                example = "Imobiliária Centro")
        @NotBlank(message = "Informe a pessoa ou empresa")
        @Size(max = 150, message = "O nome da pessoa deve ter no máximo 150 caracteres")
        String pessoa,

        @Schema(description = "CPF ou CNPJ, opcional", example = "12.345.678/0001-90", nullable = true)
        @Size(max = 30, message = "O documento deve ter no máximo 30 caracteres")
        String documento,

        @Schema(example = "Aluguel", nullable = true)
        @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres")
        String categoria,

        @Schema(description = "Valor bruto", example = "2500.00")
        @NotNull(message = "O valor é obrigatório")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @Digits(integer = 12, fraction = 2, message = "O valor deve ter até 12 dígitos inteiros e 2 decimais")
        BigDecimal valor,

        @Schema(description = "Percentual de imposto retido (0 a 100); o banco calcula imposto e líquido", example = "6.5")
        @DecimalMin(value = "0", message = "O imposto não pode ser negativo")
        @DecimalMax(value = "100", message = "O imposto deve ser no máximo 100%")
        @Digits(integer = 3, fraction = 2, message = "O imposto deve ter até 2 casas decimais")
        BigDecimal percentualImposto,

        @Schema(example = "2026-11-10")
        @NotNull(message = "O vencimento é obrigatório")
        LocalDate vencimento,

        @Schema(example = "PIX")
        @NotNull(message = "A forma de pagamento é obrigatória")
        FormaPagamento formaPagamento,

        @Schema(description = "Obrigatória quando a forma for PIX; ignorada nas demais", example = "financeiro@imobiliaria.com.br",
                nullable = true)
        @Size(max = 140, message = "A chave Pix deve ter no máximo 140 caracteres")
        String chavePix,

        @Schema(nullable = true)
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres")
        String observacoes
) {
}
