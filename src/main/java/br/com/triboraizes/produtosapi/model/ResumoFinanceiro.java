package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Totais calculados pela função {@code resumo_financeiro} do banco, sempre pelo valor líquido. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResumoFinanceiro(
        BigDecimal aPagar,
        BigDecimal aPagarVencido,
        long contasAPagarVencidas,
        BigDecimal aReceber,
        BigDecimal aReceberVencido,
        long contasAReceberVencidas,
        BigDecimal pagoNoMes,
        BigDecimal recebidoNoMes
) {
}
