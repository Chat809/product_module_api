package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.WebhookEntrada;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Webhook de entrada. O token (parte final do endereço) vem completo só ao criar ou regenerar.")
public record WebhookEntradaDTO(
        UUID id,
        String nome,
        String token,
        boolean tokenCompleto,
        String acao,
        UUID origemId,
        boolean ativo,
        int totalRecebidos,
        OffsetDateTime ultimoUsoEm,
        OffsetDateTime criadoEm
) {

    public static WebhookEntradaDTO from(WebhookEntrada entrada, boolean mostrarToken) {
        return new WebhookEntradaDTO(entrada.id(), entrada.nome(),
                mostrarToken ? entrada.token() : WebhookSaidaDTO.mascarar(entrada.token()), mostrarToken, entrada.acao(),
                entrada.origemId(), entrada.ativo(), entrada.totalRecebidos(), entrada.ultimoUsoEm(), entrada.criadoEm());
    }
}
