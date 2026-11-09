package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Linha de {@code clientes}. Lida em snake_case do banco e devolvida em camelCase pela API. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Cliente(
        UUID id,
        String tipo,
        String nome,
        String documento,
        String email,
        String telefone,
        @JsonAlias("data_nascimento") LocalDate dataNascimento,
        String cep,
        String endereco,
        String cidade,
        String uf,
        String origem,
        @JsonAlias("responsavel_id") UUID responsavelId,
        String observacoes,
        boolean ativo,
        @JsonAlias("criado_em") OffsetDateTime criadoEm,
        @JsonAlias("atualizado_em") OffsetDateTime atualizadoEm
) {
    public static final String COLUNAS = "*";
}
