package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Linha da tabela {@code public.perfis} do Supabase: dados públicos, papel, acessos e dados de "Meu perfil". */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Perfil(
        UUID id,
        String nome,
        String email,
        Papel papel,
        List<String> acessos,
        @JsonProperty("criado_em") OffsetDateTime criadoEm,
        String telefone,
        String funcao,
        String bio,
        @JsonProperty("avatar_url") String avatarUrl
) {

    public Perfil(UUID id, String nome, String email, Papel papel, List<String> acessos, OffsetDateTime criadoEm) {
        this(id, nome, email, papel, acessos, criadoEm, null, null, null, null);
    }
}
