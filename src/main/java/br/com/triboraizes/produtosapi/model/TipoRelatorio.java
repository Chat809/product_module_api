package br.com.triboraizes.produtosapi.model;

import java.util.Arrays;
import java.util.Optional;

/** Relatórios disponíveis; o caminho é o mesmo da subaba ({@code /relatorios/financeiro}). */
public enum TipoRelatorio {
    FINANCEIRO("financeiro", "Relatório financeiro", "relatorio_financeiro", true),
    COMERCIAL("comercial", "Relatório comercial", "relatorio_comercial", true),
    ESTOQUE("estoque", "Relatório de estoque", "relatorio_estoque", false),
    ATENDIMENTO("atendimento", "Relatório de atendimento", "relatorio_atendimento", true);

    private final String caminho;
    private final String titulo;
    private final String funcao;
    private final boolean usaPeriodo;

    TipoRelatorio(String caminho, String titulo, String funcao, boolean usaPeriodo) {
        this.caminho = caminho;
        this.titulo = titulo;
        this.funcao = funcao;
        this.usaPeriodo = usaPeriodo;
    }

    public String caminho() {
        return caminho;
    }

    public String titulo() {
        return titulo;
    }

    public String funcao() {
        return funcao;
    }

    public boolean usaPeriodo() {
        return usaPeriodo;
    }

    public static Optional<TipoRelatorio> doCaminho(String caminho) {
        return Arrays.stream(values()).filter(t -> t.caminho.equals(caminho)).findFirst();
    }
}
