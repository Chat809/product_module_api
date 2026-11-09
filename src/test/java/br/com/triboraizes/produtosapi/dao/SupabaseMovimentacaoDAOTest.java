package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.config.SupabaseConfig;
import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.EstoqueInsuficienteException;
import br.com.triboraizes.produtosapi.exception.ServicoDeDadosIndisponivelException;
import br.com.triboraizes.produtosapi.model.MovimentacaoEstoque;
import br.com.triboraizes.produtosapi.model.TipoMovimentacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseMovimentacaoDAOTest {

    private static final String URL = "https://projeto.supabase.co";
    private static final UUID PRODUTO = UUID.fromString("3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10");
    private static final UUID MARIA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String MOVIMENTACAO_JSON = """
            [{"id": "11cfde99-930c-4e52-80a5-ba3392bf5067", "produto_id": "3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10",
              "tipo": "SAIDA", "quantidade": 4, "saldo_anterior": 10, "saldo_apos": 6, "motivo": "Venda",
              "criado_por": "22222222-2222-2222-2222-222222222222", "criado_em": "2026-11-09T12:00:00+00:00"}]
            """;

    private MockRestServiceServer servidor;
    private SupabaseMovimentacaoDAO dao;

    @BeforeEach
    void configurar() {
        SupabaseProperties properties = new SupabaseProperties(URL, "sb_secret_teste");
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        dao = new SupabaseMovimentacaoDAO(SupabaseConfig.criarRestClient(builder, properties), properties);
    }

    @Test
    @DisplayName("Deve registrar pela função do banco, com tipo, quantidade, motivo e autor")
    void deveRegistrar() {
        servidor.expect(requestTo(URL + "/rest/v1/rpc/registrar_movimentacao"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {"p_produto_id": "3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10", "p_tipo": "SAIDA", "p_quantidade": 4,
                         "p_motivo": "Venda", "p_usuario": "22222222-2222-2222-2222-222222222222"}
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess(MOVIMENTACAO_JSON, MediaType.APPLICATION_JSON));

        MovimentacaoEstoque movimentacao = dao.registrar(PRODUTO, TipoMovimentacao.SAIDA, 4, "Venda", MARIA).orElseThrow();

        assertThat(movimentacao.produtoId()).isEqualTo(PRODUTO);
        assertThat(movimentacao.saldoAnterior()).isEqualTo(10);
        assertThat(movimentacao.saldoApos()).isEqualTo(6);
        assertThat(movimentacao.criadoPor()).isEqualTo(MARIA);
    }

    @Test
    @DisplayName("Produto inexistente: a função não devolve linha")
    void deveDevolverVazioSemProduto() {
        servidor.expect(requestTo(URL + "/rest/v1/rpc/registrar_movimentacao"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(dao.registrar(PRODUTO, TipoMovimentacao.ENTRADA, 1, null, MARIA)).isEmpty();
    }

    @Test
    @DisplayName("Saída maior que o saldo vira estoque insuficiente com o saldo informado pelo banco")
    void deveTratarEstoqueInsuficiente() {
        servidor.expect(request -> { }).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\": \"P0001\", \"details\": \"6\", \"hint\": null, \"message\": \"estoque_insuficiente\"}"));
        servidor.expect(request -> { }).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .body("{\"message\": \"estoque_insuficiente\"}"));

        assertThatThrownBy(() -> dao.registrar(PRODUTO, TipoMovimentacao.SAIDA, 99, null, MARIA))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessage("Estoque insuficiente: há 6 unidade(s) disponível(is) e a saída pedida é de 99");
        assertThatThrownBy(() -> dao.registrar(PRODUTO, TipoMovimentacao.SAIDA, 99, null, MARIA))
                .hasMessageContaining("há 0 unidade(s)");
    }

    @Test
    @DisplayName("Outros erros da função viram serviço de dados indisponível")
    void deveTratarOutrosErros() {
        servidor.expect(request -> { }).andRespond(withStatus(HttpStatus.BAD_REQUEST).body("{\"message\": \"tipo_invalido\"}"));

        assertThatThrownBy(() -> dao.registrar(PRODUTO, TipoMovimentacao.ENTRADA, 1, null, MARIA))
                .isInstanceOf(ServicoDeDadosIndisponivelException.class);
    }

    @Test
    @DisplayName("Deve listar o histórico do produto do mais recente para o mais antigo, com o total")
    void deveListarHistorico() {
        HttpHeaders total = new HttpHeaders();
        total.add("Content-Range", "2-3/4");
        servidor.expect(requestTo(URL + "/rest/v1/movimentacoes_estoque?select=*&produto_id=eq." + PRODUTO
                        + "&order=criado_em.desc,id.desc&offset=2&limit=2"))
                .andExpect(method(GET))
                .andExpect(header("Prefer", "count=exact"))
                .andRespond(withSuccess(MOVIMENTACAO_JSON, MediaType.APPLICATION_JSON).headers(total));

        MovimentacaoDAO.Pagina pagina = dao.listarPorProduto(PRODUTO, 1, 2);

        assertThat(pagina.movimentacoes()).hasSize(1);
        assertThat(pagina.movimentacoes().getFirst().tipo()).isEqualTo(TipoMovimentacao.SAIDA);
        assertThat(pagina.total()).isEqualTo(4);
    }
}
