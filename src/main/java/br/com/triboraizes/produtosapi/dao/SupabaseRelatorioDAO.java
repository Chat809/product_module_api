package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.model.Relatorio;
import br.com.triboraizes.produtosapi.model.TipoRelatorio;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** Relatórios calculados no banco pelas funções {@code relatorio_*}, que devolvem um único JSON. */
@Repository
public class SupabaseRelatorioDAO extends SupabaseTabela implements RelatorioDAO {

    public SupabaseRelatorioDAO(RestClient supabaseRestClient, SupabaseProperties properties) {
        super(supabaseRestClient, properties);
    }

    @Override
    public Relatorio gerar(TipoRelatorio tipo, Map<String, Object> parametros) {
        Relatorio relatorio = chamar(() -> restClient.post()
                .uri("/rest/v1/rpc/" + tipo.funcao())
                .contentType(MediaType.APPLICATION_JSON)
                .body(parametros)
                .retrieve()
                .body(Relatorio.class));
        return relatorio == null ? new Relatorio(List.of(), List.of(), List.of()) : relatorio;
    }
}
