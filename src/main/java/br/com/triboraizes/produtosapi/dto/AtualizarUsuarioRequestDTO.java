package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.Papel;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Dados editáveis de um usuário (somente ADMIN)")
public record AtualizarUsuarioRequestDTO(

        @Schema(example = "Maria Souza")
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,

        @Schema(example = "maria@exemplo.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
        String email,

        @Schema(example = "USUARIO")
        @NotNull(message = "O papel é obrigatório")
        Papel papel,

        @ArraySchema(arraySchema = @Schema(
                description = "Módulos liberados: [\"*\"] para todos ou uma lista com 'dashboard' e/ou 'produtos'. "
                        + "Ignorado para ADMIN, que sempre tem acesso total.",
                example = "[\"dashboard\", \"produtos\"]"))
        @NotNull(message = "Os acessos são obrigatórios")
        List<String> acessos
) {
}
