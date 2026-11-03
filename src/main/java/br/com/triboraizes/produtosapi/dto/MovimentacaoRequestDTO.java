package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.TipoMovimentacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Entrada ou saída de estoque de um produto")
public record MovimentacaoRequestDTO(

        @Schema(description = "ENTRADA ou SAIDA", example = "ENTRADA")
        @NotNull(message = "O tipo da movimentação é obrigatório")
        TipoMovimentacao tipo,

        @Schema(example = "10")
        @NotNull(message = "A quantidade é obrigatória")
        @Positive(message = "A quantidade deve ser maior que zero")
        @Max(value = 1_000_000, message = "A quantidade deve ser de no máximo 1.000.000")
        Integer quantidade,

        @Schema(example = "Reposição do fornecedor")
        @Size(max = 200, message = "O motivo deve ter no máximo 200 caracteres")
        String motivo
) {
}
