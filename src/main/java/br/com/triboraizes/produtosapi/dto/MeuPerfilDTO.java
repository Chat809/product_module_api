package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.Acessos;
import br.com.triboraizes.produtosapi.model.Papel;
import br.com.triboraizes.produtosapi.model.Perfil;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Perfil completo do usuário logado")
public record MeuPerfilDTO(
        @Schema(example = "6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e") UUID id,
        @Schema(example = "Maria Souza") String nome,
        @Schema(description = "Somente leitura: só um administrador altera o e-mail", example = "maria@exemplo.com")
        String email,
        @Schema(example = "USUARIO") Papel papel,
        @ArraySchema(arraySchema = @Schema(description = "Módulos liberados", example = "[\"dashboard\", \"produtos\", \"demandas\"]"))
        List<String> acessos,
        Instant criadoEm,
        @Schema(example = "(11) 99999-0000", nullable = true) String telefone,
        @Schema(description = "Cargo; somente leitura, definido pelo admin", example = "Desenvolvedor", nullable = true)
        String funcao,
        @Schema(example = "Cuido do estoque e das compras.", nullable = true) String bio,
        @Schema(nullable = true) String avatarUrl,
        @Schema(description = "Somente leitura, definido pelo admin", example = "Tecnologia", nullable = true)
        String departamento
) {

    public static MeuPerfilDTO from(Perfil perfil) {
        Instant criadoEm = perfil.criadoEm() == null ? null : perfil.criadoEm().toInstant();
        return new MeuPerfilDTO(perfil.id(), perfil.nome(), perfil.email(), perfil.papel(),
                Acessos.de(perfil.acessos(), perfil.papel()), criadoEm,
                perfil.telefone(), perfil.funcao(), perfil.bio(), perfil.avatarUrl(), perfil.departamento());
    }
}
