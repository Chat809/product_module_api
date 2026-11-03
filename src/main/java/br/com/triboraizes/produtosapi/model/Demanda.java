package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Linha da tabela {@code public.demandas}. As demandas em que alguém é responsável são as tarefas dessa pessoa. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Demanda(
        UUID id,
        String titulo,
        String descricao,
        StatusDemanda status,
        UrgenciaDemanda urgencia,
        TipoDemanda tipo,
        LocalDate prazo,
        List<UUID> responsaveis,
        List<ItemChecklist> checklist,
        Integer ordem,
        @JsonProperty("criado_por") UUID criadoPor,
        @JsonProperty("criado_em") OffsetDateTime criadoEm,
        @JsonProperty("atualizado_em") OffsetDateTime atualizadoEm
) {
}
