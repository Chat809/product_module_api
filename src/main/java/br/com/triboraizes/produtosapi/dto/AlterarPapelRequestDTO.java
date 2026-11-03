package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.Papel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Novo papel do usuário")
public record AlterarPapelRequestDTO(

        @Schema(example = "ADMIN")
        @NotNull(message = "O papel é obrigatório")
        Papel papel
) {
}
