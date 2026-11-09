package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Arrasto de um lead no funil")
public record MoverLeadRequestDTO(
        @NotNull(message = "Informe a etapa de destino")
        UUID etapaId,

        @Schema(description = "Posição do card na etapa; o painel envia um valor entre os vizinhos. Vazio: fim da etapa")
        Double ordem,

        @Schema(description = "Obrigatório ao mover para uma etapa do tipo Perdido")
        @Size(max = 300, message = "O motivo da perda deve ter no máximo 300 caracteres")
        String motivoPerda
) {
}
