package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados que o próprio usuário pode editar. E-mail, papel e acessos ficam com o administrador.")
public record AtualizarMeuPerfilRequestDTO(

        @Schema(example = "Maria Souza")
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,

        @Schema(example = "(11) 99999-0000", nullable = true)
        @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres")
        @Pattern(regexp = "^[0-9()+\\-\\s]*$", message = "O telefone só pode ter números, espaços, parênteses, + e -")
        String telefone,

        @Schema(example = "Analista de compras", nullable = true)
        @Size(max = 80, message = "A função deve ter no máximo 80 caracteres")
        String funcao,

        @Schema(example = "Cuido do estoque e das compras.", nullable = true)
        @Size(max = 500, message = "A bio deve ter no máximo 500 caracteres")
        String bio
) {
}
