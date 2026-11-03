package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de acesso")
public record LoginRequestDTO(

        @Schema(example = "maria@exemplo.com")
        @NotBlank(message = "O e-mail é obrigatório")
        String email,

        @Schema(example = "minhaSenha123")
        @NotBlank(message = "A senha é obrigatória")
        String senha
) {
}
