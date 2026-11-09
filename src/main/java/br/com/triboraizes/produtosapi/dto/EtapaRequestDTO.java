package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Etapa do funil de leads")
public record EtapaRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 40, message = "O nome deve ter entre 2 e 40 caracteres")
        String nome,

        @Schema(description = "Posição no funil, da esquerda para a direita")
        @Min(value = 0, message = "A ordem não pode ser negativa")
        @Max(value = 999, message = "A ordem deve ser no máximo 999")
        Integer ordem,

        @Schema(example = "#0ea5e9")
        @Pattern(regexp = "^(#[0-9a-fA-F]{6})?$", message = "Informe a cor no formato #RRGGBB")
        String cor,

        @Schema(description = "ABERTA, GANHO ou PERDIDO; padrão: ABERTA")
        @Pattern(regexp = "^(ABERTA|GANHO|PERDIDO)?$", message = "Tipo de etapa inválido")
        String tipo,

        Boolean ativo
) {
}
