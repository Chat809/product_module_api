package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Linha da tabela {@code public.perfis} do Supabase: dados públicos, papel e acessos de um usuário do Auth. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Perfil(
        UUID id,
        String nome,
        String email,
        Papel papel,
        List<String> acessos,
        @JsonProperty("criado_em") OffsetDateTime criadoEm
) {
}
