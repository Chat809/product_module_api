package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Abertura ou edição de reclamação. Informe o cliente cadastrado ou o nome de quem reclamou.")
public record ReclamacaoRequestDTO(
        UUID clienteId,

        @Size(max = 150, message = "O nome do contato deve ter no máximo 150 caracteres")
        String nomeContato,

        @Schema(description = "Telefone ou e-mail de retorno")
        @Size(max = 150, message = "O contato deve ter no máximo 150 caracteres")
        String contato,

        @NotBlank(message = "O assunto é obrigatório")
        @Size(min = 3, max = 150, message = "O assunto deve ter entre 3 e 150 caracteres")
        String assunto,

        @NotBlank(message = "Descreva a reclamação")
        @Size(max = 4000, message = "A descrição deve ter no máximo 4000 caracteres")
        String descricao,

        @Schema(description = "TELEFONE, EMAIL, WHATSAPP, PRESENCIAL, SITE ou REDES_SOCIAIS")
        @Pattern(regexp = "^(TELEFONE|EMAIL|WHATSAPP|PRESENCIAL|SITE|REDES_SOCIAIS)?$", message = "Canal inválido")
        String canal,

        @Schema(description = "BAIXA, MEDIA, ALTA ou CRITICA; padrão: MEDIA")
        @Pattern(regexp = "^(BAIXA|MEDIA|ALTA|CRITICA)?$", message = "Prioridade inválida")
        String prioridade,

        UUID responsavelId,

        LocalDate prazo
) {
}
