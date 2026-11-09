package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/** Etapa do funil de leads. {@code tipo} é ABERTA, GANHO ou PERDIDO; a conversão leva o lead à primeira etapa GANHO. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrmEtapa(UUID id, String nome, int ordem, String cor, String tipo, boolean ativo) {
    public static final String COLUNAS = "id,nome,ordem,cor,tipo,ativo";
}
