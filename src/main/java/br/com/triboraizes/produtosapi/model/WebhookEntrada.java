package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Endereço público que outro sistema chama para criar leads. O token na URL é a credencial. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookEntrada(
        UUID id,
        String nome,
        String token,
        String acao,
        @JsonAlias("origem_id") UUID origemId,
        boolean ativo,
        @JsonAlias("total_recebidos") int totalRecebidos,
        @JsonAlias("ultimo_uso_em") OffsetDateTime ultimoUsoEm,
        @JsonAlias("criado_em") OffsetDateTime criadoEm
) {
    public static final String COLUNAS = "*";
}
