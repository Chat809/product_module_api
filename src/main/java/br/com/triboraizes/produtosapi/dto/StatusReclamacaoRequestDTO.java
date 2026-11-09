package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Mudança de status de uma reclamação")
public record StatusReclamacaoRequestDTO(
        @Schema(description = "ABERTA, EM_ANALISE, RESOLVIDA ou ENCERRADA")
        @NotBlank(message = "Informe o status")
        @Pattern(regexp = "^(ABERTA|EM_ANALISE|RESOLVIDA|ENCERRADA)$", message = "Status inválido")
        String status,

        @Schema(description = "Obrigatória ao resolver")
        @Size(max = 4000, message = "A solução deve ter no máximo 4000 caracteres")
        String solucao,

        @Schema(description = "Comentário que entra no histórico junto com a mudança")
        @Size(max = 2000, message = "O comentário deve ter no máximo 2000 caracteres")
        String comentario
) {
}
