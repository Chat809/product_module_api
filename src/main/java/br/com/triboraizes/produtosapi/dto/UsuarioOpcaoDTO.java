package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Usuário que pode ser escolhido como responsável (sem e-mail ou telefone)")
public record UsuarioOpcaoDTO(
        @Schema(example = "6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e") UUID id,
        @Schema(example = "Maria Souza") String nome,
        @Schema(nullable = true) String avatarUrl
) {

    public static UsuarioOpcaoDTO from(Perfil perfil) {
        return new UsuarioOpcaoDTO(perfil.id(), perfil.nome(), perfil.avatarUrl());
    }
}
