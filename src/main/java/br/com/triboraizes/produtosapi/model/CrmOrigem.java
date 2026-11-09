package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** De onde o lead veio (site, indicação, Instagram...). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrmOrigem(UUID id, String nome, boolean ativo) {
    public static final String COLUNAS = "id,nome,ativo";
}
