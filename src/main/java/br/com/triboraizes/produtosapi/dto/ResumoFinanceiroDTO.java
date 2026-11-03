package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.ResumoFinanceiro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Totais do financeiro pelo valor líquido")
public record ResumoFinanceiroDTO(
        @Schema(description = "Total pendente a pagar", example = "4850.00") BigDecimal aPagar,
        @Schema(description = "Parte do total a pagar já vencida", example = "1200.00") BigDecimal aPagarVencido,
        @Schema(example = "2") long contasAPagarVencidas,
        @Schema(description = "Total pendente a receber", example = "9300.00") BigDecimal aReceber,
        @Schema(description = "Parte do total a receber já vencida", example = "0.00") BigDecimal aReceberVencido,
        @Schema(example = "0") long contasAReceberVencidas,
        @Schema(description = "Pago no mês atual", example = "3100.00") BigDecimal pagoNoMes,
        @Schema(description = "Recebido no mês atual", example = "7400.00") BigDecimal recebidoNoMes,
        @Schema(description = "aReceber - aPagar", example = "4450.00") BigDecimal saldoPrevisto
) {

    public static ResumoFinanceiroDTO from(ResumoFinanceiro resumo) {
        return new ResumoFinanceiroDTO(resumo.aPagar(), resumo.aPagarVencido(), resumo.contasAPagarVencidas(),
                resumo.aReceber(), resumo.aReceberVencido(), resumo.contasAReceberVencidas(),
                resumo.pagoNoMes(), resumo.recebidoNoMes(), resumo.aReceber().subtract(resumo.aPagar()));
    }
}
