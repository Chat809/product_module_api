package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.Acessos;
import br.com.triboraizes.produtosapi.model.Papel;
import br.com.triboraizes.produtosapi.model.Perfil;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Usuário do sistema")
public record UsuarioDTO(
        @Schema(example = "6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e") UUID id,
        @Schema(example = "Maria Souza") String nome,
        @Schema(example = "maria@exemplo.com") String email,
        @Schema(example = "USUARIO") Papel papel,
        @ArraySchema(arraySchema = @Schema(description = "Módulos liberados: '*' (todos) ou 'dashboard', 'produtos', 'usuarios'",
                example = "[\"dashboard\", \"produtos\"]"))
        List<String> acessos,
        @Schema(description = "Data de criação da conta; ausente na resposta do login", nullable = true) Instant criadoEm
) {

    public static UsuarioDTO from(Perfil perfil) {
        Instant criadoEm = perfil.criadoEm() == null ? null : perfil.criadoEm().toInstant();
        return new UsuarioDTO(perfil.id(), perfil.nome(), perfil.email(), perfil.papel(),
                Acessos.de(perfil.acessos(), perfil.papel()), criadoEm);
    }
}
