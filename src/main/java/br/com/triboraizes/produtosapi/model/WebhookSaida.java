package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Destino que recebe os eventos assinados. O segredo nunca sai da API sem máscara, exceto ao ser gerado. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookSaida(
        UUID id,
        String nome,
        String url,
        List<String> eventos,
        String segredo,
        boolean ativo,
        @JsonAlias("criado_em") OffsetDateTime criadoEm,
        @JsonAlias("atualizado_em") OffsetDateTime atualizadoEm
) {
    public static final String COLUNAS = "*";
}
