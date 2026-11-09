package br.com.triboraizes.produtosapi.model;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Filtros opcionais da listagem de contas. {@code vencidas} considera pendentes com vencimento antes de {@code hoje};
 * {@code vinculoId} é o fornecedor (a pagar) ou o cliente (a receber).
 */
public record FiltroContas(StatusConta status, boolean vencidas, String busca,
                           LocalDate vencimentoDe, LocalDate vencimentoAte, LocalDate hoje,
                           UUID vinculoId, UUID centroCustoId) {
}
