package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.RelatorioDAO;
import br.com.triboraizes.produtosapi.exception.RegistroNaoEncontradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.model.Relatorio;
import br.com.triboraizes.produtosapi.model.TipoRelatorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/** Gera os relatórios (calculados no banco) e a exportação em CSV. */
@Service
public class RelatorioService {

    private static final long MAXIMO_DE_DIAS = 1096;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final char SEPARADOR = ';';

    private final RelatorioDAO dao;
    private final Clock clock;
    private final int limiteDeEstoqueBaixo;

    public RelatorioService(RelatorioDAO dao, Clock clock, @Value("${app.estoque.limite-baixo:5}") int limiteDeEstoqueBaixo) {
        this.dao = dao;
        this.clock = clock;
        this.limiteDeEstoqueBaixo = limiteDeEstoqueBaixo;
    }

    public record Periodo(LocalDate inicio, LocalDate fim) {
    }

    public TipoRelatorio tipo(String caminho) {
        return TipoRelatorio.doCaminho(caminho).orElseThrow(() -> new RegistroNaoEncontradoException(
                "Relatório não encontrado", "Relatório " + caminho + " não encontrado"));
    }

    /** Sem datas, o período é o mês corrente até hoje. */
    public Periodo periodo(LocalDate inicio, LocalDate fim) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate de = inicio != null ? inicio : hoje.withDayOfMonth(1);
        LocalDate ate = fim != null ? fim : hoje;
        if (ate.isBefore(de)) {
            throw new RequisicaoInvalidaException("A data final deve ser igual ou posterior à inicial");
        }
        if (ChronoUnit.DAYS.between(de, ate) > MAXIMO_DE_DIAS) {
            throw new RequisicaoInvalidaException("O período pode ter no máximo 3 anos");
        }
        return new Periodo(de, ate);
    }

    public Relatorio gerar(TipoRelatorio tipo, Periodo periodo) {
        Map<String, Object> parametros = tipo.usaPeriodo()
                ? Map.of("p_inicio", periodo.inicio(), "p_fim", periodo.fim())
                : Map.of("p_limite", limiteDeEstoqueBaixo);
        return dao.gerar(tipo, parametros);
    }

    /**
     * CSV para abrir no Excel em português: separador {@code ;}, vírgula decimal e BOM UTF-8. Cada bloco
     * (indicadores, gráficos e tabelas) vem com seu título e uma linha em branco entre eles.
     */
    public String csv(TipoRelatorio tipo, Periodo periodo, Relatorio relatorio) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        linha(csv, List.of(tipo.titulo()));
        if (tipo.usaPeriodo()) {
            linha(csv, List.of("Período", DATA.format(periodo.inicio()) + " a " + DATA.format(periodo.fim())));
        }
        linha(csv, List.of("Gerado em", DATA.format(LocalDate.now(clock))));

        csv.append("\r\n");
        linha(csv, List.of("Indicadores"));
        for (Relatorio.Indicador indicador : relatorio.indicadores()) {
            linha(csv, List.of(indicador.rotulo(), numero(indicador.valor(), indicador.formato())));
        }
        for (Relatorio.Grafico grafico : relatorio.graficos()) {
            csv.append("\r\n");
            linha(csv, List.of(grafico.titulo()));
            for (Relatorio.Item item : grafico.itens()) {
                linha(csv, List.of(item.rotulo(), numero(item.valor(), grafico.formato())));
            }
        }
        for (Relatorio.Tabela tabela : relatorio.tabelas()) {
            csv.append("\r\n");
            linha(csv, List.of(tabela.titulo()));
            linha(csv, tabela.colunas().stream().map(Relatorio.Coluna::rotulo).toList());
            for (Map<String, Object> registro : tabela.linhas()) {
                linha(csv, tabela.colunas().stream().map(c -> celula(registro.get(c.chave()), c.formato())).toList());
            }
        }
        return csv.toString();
    }

    public String nomeDoArquivo(TipoRelatorio tipo, Periodo periodo) {
        return tipo.usaPeriodo()
                ? "relatorio-" + tipo.caminho() + "-" + periodo.inicio() + "-a-" + periodo.fim() + ".csv"
                : "relatorio-" + tipo.caminho() + "-" + LocalDate.now(clock) + ".csv";
    }

    private static void linha(StringBuilder csv, List<String> campos) {
        csv.append(campos.stream().map(RelatorioService::escapar).collect(Collectors.joining(String.valueOf(SEPARADOR))))
                .append("\r\n");
    }

    private static String celula(Object valor, String formato) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Number n && !"texto".equals(formato)) {
            return numero(new BigDecimal(n.toString()), formato);
        }
        return valor.toString();
    }

    private static String numero(BigDecimal valor, String formato) {
        if (valor == null) {
            return "";
        }
        DecimalFormat formatador = new DecimalFormat("moeda".equals(formato) ? "0.00" : "0.##",
                DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR")));
        return formatador.format(valor);
    }

    /** Aspas quando preciso e proteção contra fórmulas (texto começando com = + - @ vira texto literal). */
    private static String escapar(String campo) {
        if (campo == null) {
            return "";
        }
        String texto = campo;
        if (!texto.isEmpty() && "=+-@\t\r".indexOf(texto.charAt(0)) >= 0 && !texto.matches("-?\\d+(,\\d+)?")) {
            texto = "'" + texto;
        }
        if (texto.indexOf(SEPARADOR) >= 0 || texto.contains("\"") || texto.contains("\n") || texto.contains("\r")) {
            texto = "\"" + texto.replace("\"", "\"\"") + "\"";
        }
        return texto;
    }
}
