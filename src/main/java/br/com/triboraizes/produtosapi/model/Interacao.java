package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Registro de contato (ligação, e-mail, reunião, WhatsApp ou nota) feito com um lead ou cliente. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Interacao(
        UUID id,
        @JsonAlias("lead_id") UUID leadId,
        @JsonAlias("cliente_id") UUID clienteId,
        String tipo,
        String descricao,
        @JsonAlias("registrado_por") UUID registradoPor,
        @JsonAlias("criado_em") OffsetDateTime criadoEm
) {
    public static final String COLUNAS = "*";
}
