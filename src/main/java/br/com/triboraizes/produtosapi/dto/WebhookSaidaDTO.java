package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.WebhookSaida;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Webhook de saída. O segredo vem completo só ao criar ou regenerar; nas demais respostas, mascarado.")
public record WebhookSaidaDTO(
        UUID id,
        String nome,
        String url,
        List<String> eventos,
        String segredo,
        boolean segredoCompleto,
        boolean ativo,
        OffsetDateTime criadoEm
) {

    public static WebhookSaidaDTO from(WebhookSaida webhook, boolean mostrarSegredo) {
        return new WebhookSaidaDTO(webhook.id(), webhook.nome(), webhook.url(), webhook.eventos(),
                mostrarSegredo ? webhook.segredo() : mascarar(webhook.segredo()), mostrarSegredo, webhook.ativo(),
                webhook.criadoEm());
    }

    static String mascarar(String valor) {
        return valor == null || valor.length() < 8 ? "••••" : valor.substring(0, 4) + "••••" + valor.substring(valor.length() - 4);
    }
}
