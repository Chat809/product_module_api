package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.config.SupabaseConfig;
import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.ServicoDeDadosIndisponivelException;
import br.com.triboraizes.produtosapi.model.Demanda;
import br.com.triboraizes.produtosapi.model.FiltroDemandas;
import br.com.triboraizes.produtosapi.model.ItemChecklist;
import br.com.triboraizes.produtosapi.model.StatusDemanda;
import br.com.triboraizes.produtosapi.model.TipoDemanda;
import br.com.triboraizes.produtosapi.model.UrgenciaDemanda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseDemandaDAOTest {

    private static final String URL = "https://projeto.supabase.co";
    private static final String TABELA = URL + "/rest/v1/demandas";
    private static final UUID ID = UUID.fromString("7a2d3c1b-5e4f-4a6b-9c8d-1e2f3a4b5c6d");
    private static final UUID MARIA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String DEMANDA_JSON = """
            [{"id": "7a2d3c1b-5e4f-4a6b-9c8d-1e2f3a4b5c6d", "titulo": "Relatório", "descricao": null,
              "status": "EM_ANDAMENTO", "urgencia": "ALTA", "tipo": "MELHORIA", "prazo": "2026-11-20",
              "responsaveis": ["22222222-2222-2222-2222-222222222222"],
              "checklist": [{"texto": "Revisar", "feito": true}], "ordem": 2,
              "criado_por": "22222222-2222-2222-2222-222222222222",
              "criado_em": "2026-11-03T08:00:00+00:00", "atualizado_em": "2026-11-03T09:00:00+00:00"}]
            """;

    private MockRestServiceServer servidor;
    private SupabaseDemandaDAO dao;

    @BeforeEach
    void configurar() {
        SupabaseProperties properties = new SupabaseProperties(URL, "sb_secret_teste");
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        dao = new SupabaseDemandaDAO(SupabaseConfig.criarRestClient(builder, properties), properties);
    }

    private static Demanda nova() {
        return new Demanda(null, "Relatório", null, StatusDemanda.A_FAZER, UrgenciaDemanda.ALTA, TipoDemanda.MELHORIA,
                LocalDate.parse("2026-11-20"), List.of(MARIA), List.of(new ItemChecklist("Revisar", false)),
                0, MARIA, null, null);
    }

    @Test
    @DisplayName("Sem filtros deve listar na ordem do Kanban e ler checklist e responsáveis")
    void deveListar() {
        servidor.expect(requestTo(TABELA + "?select=*&order=ordem.asc,criado_em.asc&limit=500"))
                .andExpect(method(GET))
                .andRespond(withSuccess(DEMANDA_JSON, MediaType.APPLICATION_JSON));

        Demanda demanda = dao.listar(new FiltroDemandas(null, null, null, false)).getFirst();

        assertThat(demanda.responsaveis()).containsExactly(MARIA);
        assertThat(demanda.checklist()).containsExactly(new ItemChecklist("Revisar", true));
        assertThat(demanda.prazo()).isEqualTo(LocalDate.parse("2026-11-20"));
    }

    @Test
    @DisplayName("Filtros de responsável, abertas e busca viram filtros do PostgREST, sem caracteres de sintaxe")
    void deveFiltrar() {
        servidor.expect(request -> {
                    String uri = URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8);
                    assertThat(uri).contains("responsaveis=cs.{" + MARIA + "}")
                            .contains("status=neq.CONCLUIDO")
                            .contains("or=(titulo.ilike.*rel  x*,descricao.ilike.*rel  x*)");
                })
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        servidor.expect(request -> assertThat(request.getURI().getRawQuery()).contains("status=eq.EM_REVISAO"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(dao.listar(new FiltroDemandas(MARIA, null, "rel,(x)", true))).isEmpty();
        assertThat(dao.listar(new FiltroDemandas(null, StatusDemanda.EM_REVISAO, "  ", true))).isEmpty();
    }

    @Test
    @DisplayName("Deve buscar por id")
    void deveBuscar() {
        servidor.expect(requestTo(TABELA + "?select=*&id=eq." + ID))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(dao.buscarPorId(ID)).isEmpty();
    }

    @Test
    @DisplayName("Deve criar com o autor e devolver a linha gravada")
    void deveCriar() {
        servidor.expect(requestTo(TABELA + "?select=*"))
                .andExpect(method(POST))
                .andExpect(header("Prefer", "return=representation"))
                .andExpect(content().json("""
                        {"titulo": "Relatório", "descricao": null, "status": "A_FAZER", "urgencia": "ALTA",
                         "tipo": "MELHORIA", "prazo": "2026-11-20", "responsaveis": ["22222222-2222-2222-2222-222222222222"],
                         "checklist": [{"texto": "Revisar", "feito": false}],
                         "criado_por": "22222222-2222-2222-2222-222222222222"}
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess(DEMANDA_JSON, MediaType.APPLICATION_JSON));

        assertThat(dao.criar(nova()).id()).isEqualTo(ID);
    }

    @Test
    @DisplayName("Editar não envia autor nem ordem")
    void deveAtualizar() {
        servidor.expect(requestTo(TABELA + "?id=eq." + ID + "&select=*"))
                .andExpect(method(PATCH))
                .andExpect(request -> assertThat(((MockClientHttpRequest) request).getBodyAsString())
                        .doesNotContain("criado_por", "ordem"))
                .andRespond(withSuccess(DEMANDA_JSON, MediaType.APPLICATION_JSON));

        assertThat(dao.atualizar(ID, nova())).isPresent();
    }

    @Test
    @DisplayName("Mover deve chamar a função do banco com coluna e posição")
    void deveMover() {
        servidor.expect(requestTo(URL + "/rest/v1/rpc/mover_demanda"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"p_id": "7a2d3c1b-5e4f-4a6b-9c8d-1e2f3a4b5c6d", "p_status": "EM_ANDAMENTO", "p_posicao": 2}
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess(DEMANDA_JSON, MediaType.APPLICATION_JSON));

        assertThat(dao.mover(ID, StatusDemanda.EM_ANDAMENTO, 2)).get().extracting(Demanda::ordem).isEqualTo(2);
    }

    @Test
    @DisplayName("Excluir informa se a linha existia")
    void deveExcluir() {
        servidor.expect(requestTo(TABELA + "?id=eq." + ID + "&select=id"))
                .andExpect(method(DELETE))
                .andRespond(withSuccess("[{\"id\": \"" + ID + "\"}]", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(TABELA + "?id=eq." + ID + "&select=id"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(dao.excluir(ID)).isTrue();
        assertThat(dao.excluir(ID)).isFalse();
    }

    @Test
    @DisplayName("Falha no Supabase ou chave ausente viram serviço de dados indisponível")
    void deveTratarFalhas() {
        servidor.expect(requestTo(TABELA + "?select=*&id=eq." + ID)).andRespond(withServerError());
        assertThatThrownBy(() -> dao.buscarPorId(ID)).isInstanceOf(ServicoDeDadosIndisponivelException.class);

        SupabaseProperties semChave = new SupabaseProperties(URL, "");
        SupabaseDemandaDAO semConfiguracao = new SupabaseDemandaDAO(
                SupabaseConfig.criarRestClient(RestClient.builder(), semChave), semChave);
        assertThatThrownBy(() -> semConfiguracao.buscarPorId(ID)).hasMessageContaining("SUPABASE_SECRET_KEY");
    }
}
