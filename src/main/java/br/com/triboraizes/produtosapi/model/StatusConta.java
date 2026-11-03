package br.com.triboraizes.produtosapi.model;

/** PAGO vale para contas a pagar e RECEBIDO para contas a receber. "Vencida" não é status: sai da data. */
public enum StatusConta {
    PENDENTE,
    PAGO,
    RECEBIDO,
    CANCELADO
}
