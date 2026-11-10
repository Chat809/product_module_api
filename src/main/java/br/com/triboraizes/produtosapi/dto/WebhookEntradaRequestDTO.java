package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Webhook de entrada (cria leads)")
public record WebhookEntradaRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 80, message = "O nome deve ter entre 2 e 80 caracteres")
        String nome,

        @Schema(description = "Origem atribuída aos leads recebidos")
        UUID origemId,

        Boolean ativo
) {
}
