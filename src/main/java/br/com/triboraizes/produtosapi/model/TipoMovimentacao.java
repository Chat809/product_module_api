package br.com.triboraizes.produtosapi.model;

public enum TipoMovimentacao {
    ENTRADA,
    SAIDA,
    /** Gerado pela API quando a quantidade é alterada diretamente na edição do produto. */
    AJUSTE
}
