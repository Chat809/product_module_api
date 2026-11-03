package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.model.Demanda;
import br.com.triboraizes.produtosapi.model.FiltroDemandas;
import br.com.triboraizes.produtosapi.model.StatusDemanda;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Acessa {@code public.demandas} pelo PostgREST. A ordem dentro da coluna é mantida por funções do banco. */
@Repository
public class SupabaseDemandaDAO extends SupabaseTabela implements DemandaDAO {

    private static final String TABELA = "/rest/v1/demandas";
    private static final int LIMITE = 500;
    private static final ParameterizedTypeReference<List<Demanda>> LISTA = new ParameterizedTypeReference<>() {
    };

    public SupabaseDemandaDAO(RestClient supabaseRestClient, SupabaseProperties properties) {
        super(supabaseRestClient, properties);
    }

    @Override
    public List<Demanda> listar(FiltroDemandas filtro) {
        StringBuilder uri = new StringBuilder(TABELA + "?select=*&order=ordem.asc,criado_em.asc&limit=" + LIMITE);
        Map<String, Object> valores = new HashMap<>();
        if (filtro.responsavel() != null) {
            uri.append("&responsaveis=cs.{responsavel}");
            valores.put("responsavel", "{" + filtro.responsavel() + "}");
        }
        if (filtro.status() != null) {
            uri.append("&status=eq.{status}");
            valores.put("status", filtro.status().name());
        } else if (filtro.somenteAbertas()) {
            uri.append("&status=neq.").append(StatusDemanda.CONCLUIDO.name());
        }
        String termo = termoDeBusca(filtro.busca());
        if (!termo.isEmpty()) {
            uri.append("&or={busca}");
            valores.put("busca", "(titulo.ilike.*" + termo + "*,descricao.ilike.*" + termo + "*)");
        }
        return chamar(() -> restClient.get().uri(uri.toString(), valores).retrieve().body(LISTA));
    }

    @Override
    public Optional<Demanda> buscarPorId(UUID id) {
        return chamar(() -> primeiro(restClient.get()
                .uri(TABELA + "?select=*&id=eq.{id}", id)
                .retrieve()
                .body(LISTA)));
    }

    @Override
    public Demanda criar(Demanda demanda) {
        Map<String, Object> dados = editaveis(demanda);
        dados.put("criado_por", demanda.criadoPor());
        return chamar(() -> primeiro(restClient.post()
                .uri(TABELA + "?select=*")
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .body(LISTA))).orElseThrow();
    }

    @Override
    public Optional<Demanda> atualizar(UUID id, Demanda demanda) {
        return chamar(() -> primeiro(restClient.patch()
                .uri(TABELA + "?id=eq.{id}&select=*", id)
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(editaveis(demanda))
                .retrieve()
                .body(LISTA)));
    }

    /** Função {@code mover_demanda} do banco: renumera a coluna de destino e move o cartão na mesma transação. */
    @Override
    public Optional<Demanda> mover(UUID id, StatusDemanda status, int posicao) {
        return chamar(() -> primeiro(restClient.post()
                .uri("/rest/v1/rpc/mover_demanda")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("p_id", id, "p_status", status.name(), "p_posicao", posicao))
                .retrieve()
                .body(LISTA)));
    }

    @Override
    public boolean excluir(UUID id) {
        List<Demanda> removidas = chamar(() -> restClient.delete()
                .uri(TABELA + "?id=eq.{id}&select=id", id)
                .header("Prefer", "return=representation")
                .retrieve()
                .body(LISTA));
        return removidas != null && !removidas.isEmpty();
    }

    private static Map<String, Object> editaveis(Demanda demanda) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("titulo", demanda.titulo());
        dados.put("descricao", demanda.descricao());
        dados.put("status", demanda.status().name());
        dados.put("urgencia", demanda.urgencia().name());
        dados.put("tipo", demanda.tipo().name());
        dados.put("prazo", demanda.prazo());
        dados.put("responsaveis", demanda.responsaveis());
        dados.put("checklist", demanda.checklist());
        return dados;
    }
}
