package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.config.SupabaseConfig;
import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.CodigoDeProdutoDuplicadoException;
import br.com.triboraizes.produtosapi.exception.ServicoDeDadosIndisponivelException;
import br.com.triboraizes.produtosapi.model.FiltroProdutos;
import br.com.triboraizes.produtosapi.model.Produto;
import br.com.triboraizes.produtosapi.model.ResumoEstoque;
import br.com.triboraizes.produtosapi.model.UnidadeMedida;
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

class SupabaseProdutoDAOTest {

    private static final String URL = "https://projeto.supabase.co";
    private static final UUID ID = UUID.fromString("3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10");
    private static final UUID MARIA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String PRODUTO_JSON = """
            [{"id": "3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10", "codigo": "PER-001", "nome": "Teclado mecânico",
              "descricao": "ABNT2", "categoria": "Periféricos", "unidade": "UN", "preco": 349.90, "custo": 210.00,
              "quantidade_em_estoque": 25, "ativo": true, "criado_por": "22222222-2222-2222-2222-222222222222",
              "atualizado_por": null, "criado_em": "2026-11-09T12:00:00+00:00",
              "atualizado_em": "2026-11-09T12:00:00+00:00", "versao": 3}]
            """;

    private MockRestServiceServer servidor;
    private SupabaseProdutoDAO dao;

    @BeforeEach
    void configurar() {
        SupabaseProperties properties = new SupabaseProperties(URL, "sb_secret_teste");
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        dao = new SupabaseProdutoDAO(SupabaseConfig.criarRestClient(builder, properties), properties);
    }

    private static Produto teclado() {
        return Produto.novo("PER-001", "Teclado mecânico", "ABNT2", "Periféricos", null, new BigDecimal("349.90"),
                new BigDecimal("210.00"), 25, null, MARIA);
    }

    @Test
    @DisplayName("Deve listar a página ordenando pela coluna do banco e ler o total do Content-Range")
    void deveListar() {
        HttpHeaders total = new HttpHeaders();
        total.add("Content-Range", "20-20/21");
        servidor.expect(requestTo(URL + "/rest/v1/produtos?select=*&order=quantidade_em_estoque.desc.nullslast,id.asc"
                        + "&offset=20&limit=20"))
                .andExpect(method(GET))
                .andExpect(header("Prefer", "count=exact"))
                .andRespond(withSuccess(PRODUTO_JSON, MediaType.APPLICATION_JSON).headers(total));

        ProdutoDAO.Pagina pagina = dao.listar(new FiltroProdutos(null, null, null), 1, 20, "quantidadeEmEstoque", false);

        assertThat(pagina.total()).isEqualTo(21);
        Produto produto = pagina.produtos().getFirst();
        assertThat(produto.codigo()).isEqualTo("PER-001");
        assertThat(produto.quantidadeEmEstoque()).isEqualTo(25);
        assertThat(produto.unidade()).isEqualTo(UnidadeMedida.UN);
        assertThat(produto.custo()).isEqualByComparingTo("210.00");
        assertThat(produto.criadoPor()).isEqualTo(MARIA);
        assertThat(produto.versao()).isEqualTo(3);
    }

    @Test
    @DisplayName("Busca, status e categoria viram filtros do PostgREST, sem caracteres que alterem a sintaxe")
    void deveFiltrar() {
        servidor.expect(request -> {
                    String uri = URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8);
                    assertThat(uri).contains("order=nome.asc.nullslast,id.asc", "ativo=is.false",
                            "categoria=ilike.Periféricos",
                            "or=(codigo.ilike.*tec*,nome.ilike.*tec*,descricao.ilike.*tec*,categoria.ilike.*tec*)");
                })
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        ProdutoDAO.Pagina pagina = dao.listar(new FiltroProdutos("tec*,", false, "Periféricos"), 0, 20, "nome", true);

        assertThat(pagina.produtos()).isEmpty();
        assertThat(pagina.total()).isZero();
    }

    @Test
    @DisplayName("Deve recusar coluna de ordenação fora da lista")
    void deveRecusarOrdenacaoDesconhecida() {
        assertThatThrownBy(() -> dao.listar(new FiltroProdutos(null, null, null), 0, 20, "senha", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Página além do fim (416) devolve lista vazia com o total")
    void deveTratarPaginaAlemDoFim() {
        HttpHeaders total = new HttpHeaders();
        total.add("Content-Range", "*/3");
        servidor.expect(request -> { })
                .andRespond(withStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE).headers(total));

        ProdutoDAO.Pagina pagina = dao.listar(new FiltroProdutos(null, null, null), 9, 20, "nome", true);

        assertThat(pagina.produtos()).isEmpty();
        assertThat(pagina.total()).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve criar gravando todos os campos editáveis e o autor")
    void deveCriar() {
        servidor.expect(requestTo(URL + "/rest/v1/produtos?select=*"))
                .andExpect(method(POST))
                .andExpect(header("Prefer", "return=representation"))
                .andExpect(content().json("""
                        {"codigo": "PER-001", "nome": "Teclado mecânico", "descricao": "ABNT2", "categoria": "Periféricos",
                         "unidade": "UN", "preco": 349.90, "custo": 210.00, "quantidade_em_estoque": 25, "ativo": true,
                         "criado_por": "22222222-2222-2222-2222-222222222222",
                         "atualizado_por": "22222222-2222-2222-2222-222222222222"}
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess(PRODUTO_JSON, MediaType.APPLICATION_JSON));

        assertThat(dao.criar(teclado()).id()).isEqualTo(ID);
    }

    @Test
    @DisplayName("Código repetido (409 do índice único) vira erro de negócio na criação e na edição")
    void deveTratarCodigoDuplicado() {
        servidor.expect(request -> { }).andRespond(withStatus(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON).body("{\"code\": \"23505\"}"));
        servidor.expect(request -> { }).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> dao.criar(teclado()))
                .isInstanceOf(CodigoDeProdutoDuplicadoException.class)
                .hasMessage("Já existe um produto com o código PER-001");
        assertThatThrownBy(() -> dao.atualizar(ID, null, teclado()))
                .isInstanceOf(CodigoDeProdutoDuplicadoException.class);
    }

    @Test
    @DisplayName("Outros erros do cliente na gravação viram serviço de dados indisponível")
    void deveTratarOutrosErrosNaGravacao() {
        servidor.expect(request -> { }).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        servidor.expect(request -> { }).andRespond(withStatus(HttpStatus.CONFLICT));
        Produto semCodigo = Produto.novo(null, "Mouse", null, null, null, BigDecimal.ONE, null, 0, null, MARIA);

        assertThatThrownBy(() -> dao.criar(teclado())).isInstanceOf(ServicoDeDadosIndisponivelException.class);
        assertThatThrownBy(() -> dao.criar(semCodigo)).isInstanceOf(ServicoDeDadosIndisponivelException.class);
    }

    @Test
    @DisplayName("Editar com versão só grava se ela for a atual; sem versão, grava direto")
    void deveAtualizar() {
        servidor.expect(requestTo(URL + "/rest/v1/produtos?id=eq." + ID + "&select=*&versao=eq.3"))
                .andExpect(method(PATCH))
                .andExpect(content().json("""
                        {"codigo": "PER-001", "nome": "Teclado mecânico", "descricao": "ABNT2", "categoria": "Periféricos",
                         "unidade": "UN", "preco": 349.90, "custo": 210.00, "quantidade_em_estoque": 25, "ativo": true,
                         "atualizado_por": "22222222-2222-2222-2222-222222222222"}
                        """, JsonCompareMode.STRICT))
                .andRespond(withSuccess(PRODUTO_JSON, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL + "/rest/v1/produtos?id=eq." + ID + "&select=*"))
                .andExpect(method(PATCH))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(dao.atualizar(ID, 3L, teclado())).isPresent();
        assertThat(dao.atualizar(ID, null, teclado())).isEmpty();
    }

    @Test
    @DisplayName("Buscar e excluir por id")
    void deveBuscarEExcluir() {
        servidor.expect(requestTo(URL + "/rest/v1/produtos?select=*&id=eq." + ID))
                .andRespond(withSuccess(PRODUTO_JSON, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL + "/rest/v1/produtos?id=eq." + ID + "&select=id"))
                .andExpect(method(DELETE))
                .andRespond(withSuccess("[{\"id\": \"" + ID + "\"}]", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL + "/rest/v1/produtos?id=eq." + ID + "&select=id"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(dao.buscarPorId(ID)).isPresent();
        assertThat(dao.excluir(ID)).isTrue();
        assertThat(dao.excluir(ID)).isFalse();
    }

    @Test
    @DisplayName("Resumo chama a função do banco e devolve o valor total com 2 casas")
    void deveResumir() {
        servidor.expect(requestTo(URL + "/rest/v1/rpc/resumo_estoque"))
                .andExpect(method(POST))
                .andExpect(content().json("{\"p_limite\": 5}", JsonCompareMode.STRICT))
                .andRespond(withSuccess("""
                        {"totalProdutos": 15, "totalItens": 216, "valorTotal": 118132.3, "esgotados": 2, "estoqueBaixo": 5}
                        """, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL + "/rest/v1/rpc/resumo_estoque"))
                .andRespond(withSuccess("""
                        {"totalProdutos": 0, "totalItens": 0, "valorTotal": null, "esgotados": 0, "estoqueBaixo": 0}
                        """, MediaType.APPLICATION_JSON));

        ResumoEstoque resumo = dao.resumir(5);
        assertThat(resumo.totalProdutos()).isEqualTo(15);
        assertThat(resumo.valorTotal()).isEqualTo(new BigDecimal("118132.30"));
        assertThat(resumo.esgotados()).isEqualTo(2);
        assertThat(dao.resumir(5).valorTotal()).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("Resumo vazio vira zeros")
    void deveResumirSemResposta() {
        servidor.expect(requestTo(URL + "/rest/v1/rpc/resumo_estoque")).andRespond(withSuccess());

        assertThat(dao.resumir(5)).isEqualTo(new ResumoEstoque(0, 0, new BigDecimal("0.00"), 0, 0));
    }

    @Test
    @DisplayName("Categorias vêm em ordem, sem repetição e sem vazios")
    void deveListarCategorias() {
        servidor.expect(requestTo(URL + "/rest/v1/produtos?select=categoria&categoria=not.is.null&order=categoria.asc"
                        + "&limit=1000"))
                .andRespond(withSuccess("""
                        [{"categoria": "Acessórios"}, {"categoria": "Acessórios"}, {"categoria": null},
                         {"categoria": "Periféricos"}]
                        """, MediaType.APPLICATION_JSON));
        servidor.expect(request -> { }).andRespond(withSuccess());

        assertThat(dao.listarCategorias()).containsExactly("Acessórios", "Periféricos");
        assertThat(dao.listarCategorias()).isEmpty();
    }

    @Test
    @DisplayName("Falha no Supabase vira serviço de dados indisponível")
    void deveTratarFalhas() {
        servidor.expect(request -> { }).andRespond(withServerError());

        assertThatThrownBy(() -> dao.buscarPorId(ID)).isInstanceOf(ServicoDeDadosIndisponivelException.class);
    }

    @Test
    @DisplayName("Sem a chave secreta, nenhuma chamada é feita")
    void deveExigirChave() {
        SupabaseProperties semChave = new SupabaseProperties(URL, "");
        SupabaseProdutoDAO semConfiguracao = new SupabaseProdutoDAO(RestClient.create(URL), semChave);

        assertThatThrownBy(() -> semConfiguracao.buscarPorId(ID))
                .isInstanceOf(ServicoDeDadosIndisponivelException.class)
                .hasMessageContaining("SUPABASE_SECRET_KEY");
    }
}
