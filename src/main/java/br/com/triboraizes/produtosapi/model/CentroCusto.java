package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Linha de {@code centros_custo}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CentroCusto(
        UUID id,
        String codigo,
        String nome,
        String descricao,
        boolean ativo,
        @JsonAlias("criado_em") OffsetDateTime criadoEm,
        @JsonAlias("atualizado_em") OffsetDateTime atualizadoEm
) {
    public static final String COLUNAS = "*";
}
