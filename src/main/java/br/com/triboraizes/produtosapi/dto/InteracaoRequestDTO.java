package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Registro de interação com lead ou cliente")
public record InteracaoRequestDTO(
        @Schema(description = "LIGACAO, EMAIL, REUNIAO, WHATSAPP ou NOTA", example = "LIGACAO")
        @NotBlank(message = "Informe o tipo da interação")
        @Pattern(regexp = "^(LIGACAO|EMAIL|REUNIAO|WHATSAPP|NOTA)$", message = "Tipo de interação inválido")
        String tipo,

        @NotBlank(message = "Descreva a interação")
        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        String descricao
) {
}
