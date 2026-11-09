package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Cadastro de lead")
public record LeadRequestDTO(
        @Schema(example = "João Lima")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 150, message = "O nome deve ter entre 2 e 150 caracteres")
        String nome,

        @Size(max = 150, message = "A empresa deve ter no máximo 150 caracteres")
        String empresa,

        @Email(message = "Informe um e-mail válido")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
        String email,

        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres")
        String telefone,

        UUID origemId,

        @Schema(description = "Etapa do funil; padrão: a primeira etapa aberta")
        UUID etapaId,

        @DecimalMin(value = "0.00", message = "O valor estimado não pode ser negativo")
        @DecimalMax(value = "9999999999.99", message = "O valor estimado é grande demais")
        BigDecimal valorEstimado,

        UUID responsavelId,

        @Size(max = 200, message = "A próxima ação deve ter no máximo 200 caracteres")
        String proximaAcao,

        LocalDate proximaAcaoEm,

        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres")
        String observacoes
) {
}
