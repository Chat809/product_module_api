package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Retorno do login com Google (fluxo PKCE)")
public record GoogleLoginRequestDTO(

        @Schema(description = "Parâmetro 'code' que o Supabase anexa ao endereço de retorno do painel",
                example = "5f0e4c1a-8b2d-4e7f-9a3c-1d2e3f4a5b6c")
        @NotBlank(message = "O código de autorização é obrigatório")
        @Size(max = 512, message = "O código de autorização deve ter no máximo 512 caracteres")
        String codigo,

        @Schema(description = "code_verifier gerado pelo painel antes de ir ao Google; o Supabase confere "
                + "se o hash dele bate com o code_challenge enviado no início",
                example = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk")
        @NotBlank(message = "O verificador é obrigatório")
        @Size(min = 43, max = 128, message = "O verificador deve ter entre 43 e 128 caracteres")
        String verificador
) {
}
