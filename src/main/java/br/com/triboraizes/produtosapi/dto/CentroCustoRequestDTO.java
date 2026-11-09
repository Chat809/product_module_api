package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Cadastro de centro de custo")
public record CentroCustoRequestDTO(
        @Schema(example = "MKT")
        @NotBlank(message = "O código é obrigatório")
        @Pattern(regexp = "^\\s*[A-Za-z0-9][A-Za-z0-9._-]{0,19}\\s*$",
                message = "Use até 20 letras, números ou . _ -, começando por letra ou número")
        String codigo,

        @Schema(example = "Marketing")
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 80, message = "O nome deve ter entre 2 e 80 caracteres")
        String nome,

        @Size(max = 300, message = "A descrição deve ter no máximo 300 caracteres")
        String descricao,

        Boolean ativo
) {
}
