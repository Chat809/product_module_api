package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.Papel;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Nova conta criada por um administrador; a pessoa já pode entrar com o e-mail e a senha informados")
public record CriarUsuarioRequestDTO(

        @Schema(example = "Maria Souza")
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres")
        String nome,

        @Schema(example = "maria@exemplo.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
        String email,

        @Schema(example = "senhaInicial1")
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres")
        String senha,

        @Schema(description = "Nível de acesso no sistema", example = "USUARIO")
        @NotNull(message = "O nível de acesso é obrigatório")
        Papel papel,

        @ArraySchema(arraySchema = @Schema(
                description = "Módulos liberados: [\"*\"] para todos ou uma lista de módulos. "
                        + "Ignorado para ADMIN, que sempre tem acesso total.",
                example = "[\"dashboard\", \"produtos\", \"demandas\"]"))
        @NotNull(message = "Os acessos são obrigatórios")
        List<String> acessos,

        @Schema(description = "Departamento na empresa", example = "Tecnologia", nullable = true)
        @Size(max = 80, message = "O departamento deve ter no máximo 80 caracteres")
        String departamento,

        @Schema(description = "Cargo na empresa", example = "Desenvolvedor", nullable = true)
        @Size(max = 80, message = "O cargo deve ter no máximo 80 caracteres")
        String funcao
) {
}
