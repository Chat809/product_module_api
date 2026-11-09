package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Linha de {@code fornecedores}. Lida em snake_case do banco e devolvida em camelCase pela API. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Fornecedor(
        UUID id,
        @JsonAlias("razao_social") String razaoSocial,
        @JsonAlias("nome_fantasia") String nomeFantasia,
        String documento,
        String contato,
        String email,
        String telefone,
        @JsonAlias("chave_pix") String chavePix,
        String categoria,
        String observacoes,
        boolean ativo,
        @JsonAlias("criado_em") OffsetDateTime criadoEm,
        @JsonAlias("atualizado_em") OffsetDateTime atualizadoEm
) {
    public static final String COLUNAS = "*";
}
