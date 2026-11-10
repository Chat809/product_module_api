package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Registro de uma entrega de webhook (já com as novas tentativas), guardado por 30 dias. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookEntrega(
        UUID id,
        @JsonAlias("webhook_id") UUID webhookId,
        String evento,
        Object payload,
        @JsonAlias("status_http") Integer statusHttp,
        boolean sucesso,
        int tentativas,
        String erro,
        @JsonAlias("duracao_ms") Integer duracaoMs,
        @JsonAlias("criado_em") OffsetDateTime criadoEm
) {
    public static final String COLUNAS = "*";
}
