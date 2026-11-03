package br.com.triboraizes.produtosapi.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Item do checklist de uma demanda")
public record ItemChecklist(
        @Schema(example = "Revisar textos da tela")
        @NotBlank(message = "O texto do item é obrigatório")
        @Size(max = 200, message = "Cada item deve ter no máximo 200 caracteres")
        String texto,

        @Schema(example = "false")
        Boolean feito
) {

    public ItemChecklist {
        feito = Boolean.TRUE.equals(feito);
    }
}
