package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.TipoNotificacao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Preferência de um tipo de notificação")
public record PreferenciaNotificacaoDTO(
        @NotNull(message = "Informe o tipo") TipoNotificacao tipo,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) String descricao,
        boolean noApp,
        @Schema(description = "Só tem efeito quando o envio de e-mail estiver habilitado no servidor") boolean porEmail
) {
}
