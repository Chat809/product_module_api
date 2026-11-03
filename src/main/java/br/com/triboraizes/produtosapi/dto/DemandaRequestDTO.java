package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.ItemChecklist;
import br.com.triboraizes.produtosapi.model.StatusDemanda;
import br.com.triboraizes.produtosapi.model.TipoDemanda;
import br.com.triboraizes.produtosapi.model.UrgenciaDemanda;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados para criar ou editar uma demanda")
public record DemandaRequestDTO(

        @Schema(example = "Ajustar relatório de estoque")
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        String titulo,

        @Schema(example = "Incluir a coluna de estoque mínimo no relatório.", nullable = true)
        @Size(max = 5000, message = "A descrição deve ter no máximo 5000 caracteres")
        String descricao,

        @Schema(example = "MELHORIA")
        @NotNull(message = "O tipo é obrigatório")
        TipoDemanda tipo,

        @Schema(example = "ALTA")
        @NotNull(message = "A urgência é obrigatória")
        UrgenciaDemanda urgencia,

        @Schema(description = "Coluna do Kanban; padrão A_FAZER ao criar", example = "A_FAZER", nullable = true)
        StatusDemanda status,

        @Schema(example = "2026-11-20", nullable = true)
        LocalDate prazo,

        @ArraySchema(arraySchema = @Schema(description = "Ids dos responsáveis (veja GET /api/v1/usuarios/opcoes)"))
        @Size(max = 20, message = "No máximo 20 responsáveis")
        List<UUID> responsaveis,

        @ArraySchema(arraySchema = @Schema(description = "Itens do checklist"))
        @Size(max = 50, message = "No máximo 50 itens no checklist")
        List<@Valid ItemChecklist> checklist
) {
}
