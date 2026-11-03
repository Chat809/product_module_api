package br.com.triboraizes.produtosapi.model;

import java.util.Arrays;

/**
 * Contas a pagar e a receber têm o mesmo formato; mudam a tabela, o nome da pessoa
 * (favorecido ou pagador), a coluna da data de baixa e o status de quitação.
 */
public enum TipoConta {
    PAGAR("contas-pagar", "contas_pagar", "favorecido", "data_pagamento", StatusConta.PAGO),
    RECEBER("contas-receber", "contas_receber", "pagador", "data_recebimento", StatusConta.RECEBIDO);

    private final String caminho;
    private final String tabela;
    private final String colunaPessoa;
    private final String colunaBaixa;
    private final StatusConta statusQuitado;

    TipoConta(String caminho, String tabela, String colunaPessoa, String colunaBaixa, StatusConta statusQuitado) {
        this.caminho = caminho;
        this.tabela = tabela;
        this.colunaPessoa = colunaPessoa;
        this.colunaBaixa = colunaBaixa;
        this.statusQuitado = statusQuitado;
    }

    public String caminho() {
        return caminho;
    }

    public String tabela() {
        return tabela;
    }

    public String colunaPessoa() {
        return colunaPessoa;
    }

    public String colunaBaixa() {
        return colunaBaixa;
    }

    public StatusConta statusQuitado() {
        return statusQuitado;
    }

    /** O controller só aceita os dois caminhos válidos, então aqui não há valor desconhecido. */
    public static TipoConta doCaminho(String caminho) {
        return Arrays.stream(values()).filter(tipo -> tipo.caminho.equals(caminho)).findFirst().orElseThrow();
    }
}
