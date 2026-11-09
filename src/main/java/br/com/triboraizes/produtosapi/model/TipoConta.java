package br.com.triboraizes.produtosapi.model;

import java.util.Arrays;

/**
 * Contas a pagar e a receber têm o mesmo formato; mudam a tabela, o nome da pessoa
 * (favorecido ou pagador), a coluna da data de baixa e o status de quitação.
 */
public enum TipoConta {
    PAGAR("contas-pagar", "contas_pagar", "favorecido", "data_pagamento", StatusConta.PAGO, "fornecedor_id"),
    RECEBER("contas-receber", "contas_receber", "pagador", "data_recebimento", StatusConta.RECEBIDO, "cliente_id");

    private final String caminho;
    private final String tabela;
    private final String colunaPessoa;
    private final String colunaBaixa;
    private final StatusConta statusQuitado;
    private final String colunaVinculo;

    TipoConta(String caminho, String tabela, String colunaPessoa, String colunaBaixa, StatusConta statusQuitado,
              String colunaVinculo) {
        this.caminho = caminho;
        this.tabela = tabela;
        this.colunaPessoa = colunaPessoa;
        this.colunaBaixa = colunaBaixa;
        this.statusQuitado = statusQuitado;
        this.colunaVinculo = colunaVinculo;
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

    /** Cadastro ligado à conta: fornecedor (a pagar) ou cliente (a receber); {@code null} se não houver. */
    public String colunaVinculo() {
        return colunaVinculo;
    }

    public StatusConta statusQuitado() {
        return statusQuitado;
    }

    /** O controller só aceita os dois caminhos válidos, então aqui não há valor desconhecido. */
    public static TipoConta doCaminho(String caminho) {
        return Arrays.stream(values()).filter(tipo -> tipo.caminho.equals(caminho)).findFirst().orElseThrow();
    }
}
