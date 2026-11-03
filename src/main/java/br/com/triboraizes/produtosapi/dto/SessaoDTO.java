package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Sessão aberta no login ou no registro")
public record SessaoDTO(
        @Schema(description = "JWT a enviar em 'Authorization: Bearer <token>'") String accessToken,
        @Schema(description = "Use em POST /api/v1/auth/refresh quando o accessToken expirar") String refreshToken,
        @Schema(description = "Momento em que o accessToken expira") Instant expiraEm,
        UsuarioDTO usuario
) {
}
