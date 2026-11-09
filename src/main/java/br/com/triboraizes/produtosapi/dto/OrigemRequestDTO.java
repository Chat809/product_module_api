package br.com.triboraizes.produtosapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrigemRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 40, message = "O nome deve ter entre 2 e 40 caracteres")
        String nome,

        Boolean ativo
) {
}
