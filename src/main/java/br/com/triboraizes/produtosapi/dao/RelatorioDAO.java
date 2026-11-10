package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.model.Relatorio;
import br.com.triboraizes.produtosapi.model.TipoRelatorio;

import java.util.Map;

public interface RelatorioDAO {

    /** Chama a função do banco do relatório com os parâmetros já no formato dela ({@code p_inicio}, {@code p_fim}...). */
    Relatorio gerar(TipoRelatorio tipo, Map<String, Object> parametros);
}
