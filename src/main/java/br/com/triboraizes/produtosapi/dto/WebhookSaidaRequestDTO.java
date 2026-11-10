package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Webhook de saída")
public record WebhookSaidaRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 80, message = "O nome deve ter entre 2 e 80 caracteres")
        String nome,

        @Schema(example = "https://exemplo.com.br/receber")
        @NotBlank(message = "A URL é obrigatória")
        @Size(max = 500, message = "A URL deve ter no máximo 500 caracteres")
        @Pattern(regexp = "^https://\\S+$", message = "Use uma URL https://")
        String url,

        @Schema(description = "Eventos que disparam o envio", example = "[\"lead.criado\"]")
        @NotEmpty(message = "Escolha ao menos um evento")
        List<String> eventos,

        Boolean ativo
) {
}
