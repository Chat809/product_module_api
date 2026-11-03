package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.StatusDemanda;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Destino do cartão ao arrastar no Kanban")
public record MoverDemandaRequestDTO(

        @Schema(example = "EM_ANDAMENTO")
        @NotNull(message = "O status é obrigatório")
        StatusDemanda status,

        @Schema(description = "Posição na coluna de destino, começando em 0", example = "0")
        @NotNull(message = "A posição é obrigatória")
        @Min(value = 0, message = "A posição deve ser maior ou igual a 0")
        @Max(value = 10000, message = "A posição deve ser no máximo 10000")
        Integer posicao
) {
}
