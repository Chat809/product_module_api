package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Histórico de uma reclamação: abertura e mudanças de status (gravadas pelo banco) e comentários. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReclamacaoEvento(
        UUID id,
        @JsonAlias("reclamacao_id") UUID reclamacaoId,
        String tipo,
        @JsonAlias("status_anterior") String statusAnterior,
        @JsonAlias("status_novo") String statusNovo,
        String comentario,
        @JsonAlias("autor_id") UUID autorId,
        @JsonAlias("criado_em") OffsetDateTime criadoEm
) {
    public static final String COLUNAS = "*";
}
