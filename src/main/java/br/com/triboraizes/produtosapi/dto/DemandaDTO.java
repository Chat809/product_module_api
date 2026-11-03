package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.Demanda;
import br.com.triboraizes.produtosapi.model.ItemChecklist;
import br.com.triboraizes.produtosapi.model.StatusDemanda;
import br.com.triboraizes.produtosapi.model.TipoDemanda;
import br.com.triboraizes.produtosapi.model.UrgenciaDemanda;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Demanda do Kanban")
public record DemandaDTO(
        UUID id,
        @Schema(example = "Ajustar relatório de estoque") String titulo,
        String descricao,
        @Schema(example = "EM_ANDAMENTO") StatusDemanda status,
        @Schema(example = "ALTA") UrgenciaDemanda urgencia,
        @Schema(example = "MELHORIA") TipoDemanda tipo,
        @Schema(example = "2026-11-20") LocalDate prazo,
        @Schema(description = "Prazo já passou e a demanda não foi concluída") boolean atrasada,
        List<UUID> responsaveis,
        List<ItemChecklist> checklist,
        @Schema(description = "Posição na coluna") int ordem,
        UUID criadoPor,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public static DemandaDTO from(Demanda demanda, LocalDate hoje) {
        boolean atrasada = demanda.prazo() != null && demanda.prazo().isBefore(hoje)
                && demanda.status() != StatusDemanda.CONCLUIDO;
        return new DemandaDTO(demanda.id(), demanda.titulo(), demanda.descricao(), demanda.status(),
                demanda.urgencia(), demanda.tipo(), demanda.prazo(), atrasada,
                demanda.responsaveis() == null ? List.of() : demanda.responsaveis(),
                demanda.checklist() == null ? List.of() : demanda.checklist(),
                demanda.ordem() == null ? 0 : demanda.ordem(), demanda.criadoPor(),
                demanda.criadoEm() == null ? null : demanda.criadoEm().toInstant(),
                demanda.atualizadoEm() == null ? null : demanda.atualizadoEm().toInstant());
    }
}
