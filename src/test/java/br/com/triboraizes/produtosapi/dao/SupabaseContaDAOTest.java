package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.config.SupabaseConfig;
import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.ServicoDeDadosIndisponivelException;
import br.com.triboraizes.produtosapi.model.Conta;
import br.com.triboraizes.produtosapi.model.FiltroContas;
import br.com.triboraizes.produtosapi.model.FormaPagamento;
import br.com.triboraizes.produtosapi.model.ResumoFinanceiro;
import br.com.triboraizes.produtosapi.model.StatusConta;
import br.com.triboraizes.produtosapi.model.TipoConta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseContaDAOTest {

    private static final String URL = "https://projeto.supabase.co";
    private static final UUID ID = UUID.fromString("8c3e4d2a-6f5b-4c7d-8e9f-2a3b4c5d6e7f");
    private static final UUID MARIA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final LocalDate HOJE = LocalDate.parse("2026-11-03");
    private static final String COLUNAS_PAGAR = "id,descricao,pessoa:favorecido,documento,categoria,valor,percentual_imposto,"
            + "valor_imposto,valor_liquido,vencimento,data_baixa:data_pagamento,status,forma_pagamento,chave_pix,observacoes,"
            + "criado_por,criado_em,atualizado_em";
    private static final String CONTA_JSON = """
            [{"id": "8c3e4d2a-6f5b-4c7d-8e9f-2a3b4c5d6e7f", "descricao": "Aluguel", "pessoa": "Imobiliária",
              "documento": null, "categoria": "Aluguel", "valor": 2500.00, "percentual_imposto": 6.50,
              "valor_imposto": 162.50, "valor_liquido": 2337.50, "vencimento": "2026-11-10", "data_baixa": null,
              "status": "PENDENTE", "forma_pagamento": "PIX", "chave_pix": "pix@imobiliaria.com", "observacoes": null,
              "criado_por": "22222222-2222-2222-2222-222222222222", "criado_em": "2026-11-03T08:00:00+00:00",
              "atualizado_em": "2026-11-03T08:00:00+00:00"}]
            """;

    private MockRestServiceServer servidor;
    private SupabaseContaDAO dao;

    @BeforeEach
    void configurar() {
        SupabaseProperties properties = new SupabaseProperties(URL, "sb_secret_teste");
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        dao = new SupabaseContaDAO(SupabaseConfig.criarRestClient(builder, properties), properties);
    }

    private static Conta lancamento() {
        return new Conta(null, "Aluguel", "Imobiliária", null, "Aluguel", new BigDecimal("2500.00"), new BigDecimal("6.50"),
                null, null, LocalDate.parse("2026-11-10"), null, null, FormaPagamento.PIX, "pix@imobiliaria.com",
                null, MARIA, null, null);
    }

    private static FiltroContas semFiltro() {
        return new FiltroContas(null, false, null, null, null, HOJE);
    }

    @Test
    @DisplayName("Deve listar a página com alias de colunas e ler o total do Content-Range")
    void deveListar() {
        HttpHeaders total = new HttpHeaders();
        total.add("Content-Range", "20-20/21");
        servidor.expect(requestTo(URL + "/rest/v1/contas_pagar?select=" + COLUNAS_PAGAR
                        + "&order=vencimento.asc,criado_em.asc&offset=20&limit=20"))
                .andExpect(method(GET))
                .andExpect(header("Prefer", "count=exact"))
                .andRespond(withSuccess(CONTA_JSON, MediaType.APPLICATION_JSON).headers(total));

        ContaDAO.Pagina pagina = dao.listar(TipoConta.PAGAR, semFiltro(), 1, 20);

        assertThat(pagina.total()).isEqualTo(21);
        Conta conta = pagina.contas().getFirst();
        assertThat(conta.pessoa()).isEqualTo("Imobiliária");
        assertThat(conta.valorLiquido()).isEqualByComparingTo("2337.50");
        assertThat(conta.formaPagamento()).isEqualTo(FormaPagamento.PIX);
    }

    @Test
    @DisplayName("Contas a receber usam pagador e data_recebimento; filtros viram parâmetros do PostgREST")
    void deveFiltrarContasAReceber() {
        servidor.expect(request -> {
                    String uri = URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8);
                    assertThat(request.getURI().getPath()).isEqualTo("/rest/v1/contas_receber");
                    assertThat(uri).contains("pessoa:pagador", "data_baixa:data_recebimento",
                            "status=eq.PENDENTE", "vencimento=lt.2026-11-03", "vencimento=gte.2026-11-01",
                            "vencimento=lte.2026-11-30",
                            "or=(descricao.ilike.*cli*,pagador.ilike.*cli*,categoria.ilike.*cli*)");
                })
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        servidor.expect(request -> assertThat(request.getURI().getRawQuery()).contains("status=eq.RECEBIDO"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        dao.listar(TipoConta.RECEBER, new FiltroContas(StatusConta.RECEBIDO, true, "cli*", LocalDate.parse("2026-11-01"),
                LocalDate.parse("2026-11-30"), HOJE), 0, 20);
        assertThat(dao.listar(TipoConta.RECEBER, new FiltroContas(StatusConta.RECEBIDO, false, null, null, null, HOJE), 0, 20)
                .total()).isZero();
    }

    @Test
    @DisplayName("Página além do fim (416) devolve lista vazia com o total")
    void deveTratarPaginaAlemDoFim() {
        HttpHeaders total = new HttpHeaders();
        total.add("Content-Range", "*/3");
        servidor.expect(request -> { })
                .andRespond(withStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).headers(total));

        ContaDAO.Pagina pagina = dao.listar(TipoConta.PAGAR, semFiltro(), 9, 20);

        assertThat(pagina.contas()).isEmpty();
        assertThat(pagina.total()).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve criar gravando favorecido, percentual e autor (imposto e líquido ficam com o banco)")
    void deveCriar() {
        servidor.expect(requestTo(URL + "/rest/v1/contas_pagar?select=" + COLUNAS_PAGAR))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"descricao": "Aluguel", "favorecido": "Imobiliária", "documento": null, "categoria": "Aluguel",
                         "valor": 2500.00, "percentual_imposto": 6.50, "vencimento": "2026-11-10",
                         "forma_pagamento": "PIX", "chave_pix": "pix@imobiliaria.com", "observacoes": null,
                         "criado_por": "22222222-2222-2222-2222-222222222222"}
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess(CONTA_JSON, MediaType.APPLICATION_JSON));

        assertThat(dao.criar(TipoConta.PAGAR, lancamento()).id()).isEqualTo(ID);
    }

    @Test
    @DisplayName("Editar e dar baixa gravam nas colunas certas de cada tabela")
    void deveAtualizarEBaixar() {
        servidor.expect(requestTo(URL + "/rest/v1/contas_pagar?id=eq." + ID + "&select=" + COLUNAS_PAGAR))
                .andExpect(method(PATCH))
                .andRespond(withSuccess(CONTA_JSON, MediaType.APPLICATION_JSON));
        servidor.expect(request -> assertThat(request.getURI().getPath()).isEqualTo("/rest/v1/contas_receber"))
                .andExpect(content().json("{\"status\": \"RECEBIDO\", \"data_recebimento\": \"2026-11-03\"}",
                        JsonCompareMode.STRICT))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(dao.atualizar(TipoConta.PAGAR, ID, lancamento())).isPresent();
        assertThat(dao.alterarStatus(TipoConta.RECEBER, ID, StatusConta.RECEBIDO, HOJE)).isEmpty();
    }

    @Test
    @DisplayName("Buscar e excluir por id")
    void deveBuscarEExcluir() {
        servidor.expect(requestTo(URL + "/rest/v1/contas_pagar?select=" + COLUNAS_PAGAR + "&id=eq." + ID))
                .andRespond(withSuccess(CONTA_JSON, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL + "/rest/v1/contas_pagar?id=eq." + ID + "&select=id"))
                .andExpect(method(DELETE))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(dao.buscarPorId(TipoConta.PAGAR, ID)).isPresent();
        assertThat(dao.excluir(TipoConta.PAGAR, ID)).isFalse();
    }

    @Test
    @DisplayName("Resumo chama a função do banco com a data de hoje")
    void deveResumir() {
        servidor.expect(requestTo(URL + "/rest/v1/rpc/resumo_financeiro"))
                .andExpect(method(POST))
                .andExpect(content().json("{\"p_hoje\": \"2026-11-03\"}", JsonCompareMode.STRICT))
                .andRespond(withSuccess("""
                        {"aPagar": 100.5, "aPagarVencido": 0, "contasAPagarVencidas": 0, "aReceber": 300,
                         "aReceberVencido": 50, "contasAReceberVencidas": 1, "pagoNoMes": 10, "recebidoNoMes": 20}
                        """, MediaType.APPLICATION_JSON));

        ResumoFinanceiro resumo = dao.resumo(HOJE);

        assertThat(resumo.aPagar()).isEqualByComparingTo("100.5");
        assertThat(resumo.contasAReceberVencidas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Falha no Supabase vira serviço de dados indisponível")
    void deveTratarFalhas() {
        servidor.expect(request -> { }).andRespond(withServerError());

        assertThatThrownBy(() -> dao.listar(TipoConta.PAGAR, semFiltro(), 0, 20))
                .isInstanceOf(ServicoDeDadosIndisponivelException.class);
    }
}
