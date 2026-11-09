package br.com.triboraizes.produtosapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComentarioRequestDTO(
        @NotBlank(message = "Escreva o comentário")
        @Size(max = 2000, message = "O comentário deve ter no máximo 2000 caracteres")
        String comentario
) {
}
