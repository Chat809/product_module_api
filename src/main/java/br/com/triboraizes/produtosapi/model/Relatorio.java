package br.com.triboraizes.produtosapi.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Resultado de um relatório, no mesmo formato para todos os tipos: indicadores (cartões), gráficos de barras e
 * tabelas. {@code formato} diz como exibir o número: {@code moeda}, {@code numero}, {@code percentual},
 * {@code dias} ou {@code texto}.
 */
@Schema(description = "Relatório: indicadores, gráficos de barras e tabelas")
public record Relatorio(List<Indicador> indicadores, List<Grafico> graficos, List<Tabela> tabelas) {

    public record Indicador(String rotulo, BigDecimal valor, String formato) {
    }

    public record Grafico(String titulo, String formato, List<Item> itens) {
    }

    public record Item(String rotulo, BigDecimal valor) {
    }

    public record Tabela(String titulo, List<Coluna> colunas, List<Map<String, Object>> linhas) {
    }

    public record Coluna(String chave, String rotulo, String formato) {
    }

    public Relatorio {
        indicadores = indicadores == null ? List.of() : indicadores;
        graficos = graficos == null ? List.of() : graficos;
        tabelas = tabelas == null ? List.of() : tabelas;
    }
}
