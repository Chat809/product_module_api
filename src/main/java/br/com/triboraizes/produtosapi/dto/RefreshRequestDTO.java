package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Token de renovação recebido no login")
public record RefreshRequestDTO(

        @NotBlank(message = "O refreshToken é obrigatório")
        String refreshToken
) {
}
