package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Preferências de notificação do usuário")
public record PreferenciasNotificacaoDTO(
        @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "Se o servidor tem envio de e-mail configurado")
        boolean emailDisponivel,

        @NotNull(message = "Informe as preferências") List<@Valid PreferenciaNotificacaoDTO> preferencias
) {
}
