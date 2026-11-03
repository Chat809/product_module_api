package br.com.triboraizes.produtosapi.model;

import java.time.LocalDate;

/** Filtros opcionais da listagem de contas. {@code vencidas} considera pendentes com vencimento antes de {@code hoje}. */
public record FiltroContas(StatusConta status, boolean vencidas, String busca,
                           LocalDate vencimentoDe, LocalDate vencimentoAte, LocalDate hoje) {
}
