package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Linha de {@code contas_pagar} ou {@code contas_receber}. A consulta renomeia favorecido/pagador para
 * {@code pessoa} e data_pagamento/data_recebimento para {@code data_baixa}. Imposto e líquido são colunas
 * geradas pelo banco.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Conta(
        UUID id,
        String descricao,
        String pessoa,
        String documento,
        String categoria,
        BigDecimal valor,
        @JsonProperty("percentual_imposto") BigDecimal percentualImposto,
        @JsonProperty("valor_imposto") BigDecimal valorImposto,
        @JsonProperty("valor_liquido") BigDecimal valorLiquido,
        LocalDate vencimento,
        @JsonProperty("data_baixa") LocalDate dataBaixa,
        StatusConta status,
        @JsonProperty("forma_pagamento") FormaPagamento formaPagamento,
        @JsonProperty("chave_pix") String chavePix,
        String observacoes,
        @JsonProperty("criado_por") UUID criadoPor,
        @JsonProperty("criado_em") OffsetDateTime criadoEm,
        @JsonProperty("atualizado_em") OffsetDateTime atualizadoEm
) {
}
