package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.ProdutoRequestDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoResponseDTO;
import br.com.triboraizes.produtosapi.exception.ProdutoNaoEncontradoException;
import br.com.triboraizes.produtosapi.service.ProdutoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProdutoController.class)
class ProdutoControllerTest {

    private static final String URL = "/api/v1/produtos";

    private static final String JSON_VALIDO = """
            {"nome": "Teclado", "descricao": "Mecânico", "preco": 349.90, "quantidadeEmEstoque": 25}
            """;

    private static final ProdutoResponseDTO RESPONSE =
            new ProdutoResponseDTO(1L, "Teclado", "Mecânico", new BigDecimal("349.90"), 25);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProdutoService produtoService;

    @Test
    @DisplayName("GET /produtos deve retornar 200 com a lista de produtos")
    void deveListarProdutos() throws Exception {
        when(produtoService.listarTodos()).thenReturn(List.of(RESPONSE));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Teclado"));
    }

    @Test
    @DisplayName("GET /produtos/{id} deve retornar 200 com o produto")
    void deveBuscarProdutoPorId() throws Exception {
        when(produtoService.buscarPorId(1L)).thenReturn(RESPONSE);

        mockMvc.perform(get(URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(349.90))
                .andExpect(jsonPath("$.quantidadeEmEstoque").value(25));
    }

    @Test
    @DisplayName("GET /produtos/{id} deve retornar 404 quando o produto não existir")
    void deveRetornar404QuandoProdutoNaoExistir() throws Exception {
        when(produtoService.buscarPorId(99L)).thenThrow(new ProdutoNaoEncontradoException(99L));

        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Produto não encontrado"))
                .andExpect(jsonPath("$.detail").value("Produto com id 99 não encontrado"));
    }

    @Test
    @DisplayName("GET /produtos/{id} deve retornar 400 quando o id não for numérico")
    void deveRetornar400QuandoIdForInvalido() throws Exception {
        mockMvc.perform(get(URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"));
    }

    @Test
    @DisplayName("POST /produtos deve retornar 201 com header Location e o produto criado")
    void deveCriarProduto() throws Exception {
        when(produtoService.criar(any(ProdutoRequestDTO.class))).thenReturn(RESPONSE);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/produtos/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /produtos deve retornar 400 com a lista de campos inválidos")
    void deveRetornar400QuandoDadosForemInvalidos() throws Exception {
        String jsonInvalido = """
                {"nome": " ", "preco": 0, "quantidadeEmEstoque": -1}
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(jsonInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros", hasSize(3)))
                .andExpect(jsonPath("$.erros[?(@.campo == 'nome')].mensagem").value("O nome é obrigatório"));

        verifyNoInteractions(produtoService);
    }

    @Test
    @DisplayName("POST /produtos deve retornar 400 quando o JSON estiver malformado")
    void deveRetornar400QuandoJsonForMalformado() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"nome\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"));
    }

    @Test
    @DisplayName("PUT /produtos/{id} deve retornar 200 com o produto atualizado")
    void deveAtualizarProduto() throws Exception {
        when(produtoService.atualizar(eq(1L), any(ProdutoRequestDTO.class))).thenReturn(RESPONSE);

        mockMvc.perform(put(URL + "/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Teclado"));
    }

    @Test
    @DisplayName("PUT /produtos/{id} deve retornar 404 quando o produto não existir")
    void deveRetornar404AoAtualizarProdutoInexistente() throws Exception {
        when(produtoService.atualizar(eq(99L), any(ProdutoRequestDTO.class)))
                .thenThrow(new ProdutoNaoEncontradoException(99L));

        mockMvc.perform(put(URL + "/99").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /produtos/{id} deve retornar 204")
    void deveRemoverProduto() throws Exception {
        mockMvc.perform(delete(URL + "/1"))
                .andExpect(status().isNoContent());

        verify(produtoService).remover(1L);
    }

    @Test
    @DisplayName("DELETE /produtos/{id} deve retornar 404 quando o produto não existir")
    void deveRetornar404AoRemoverProdutoInexistente() throws Exception {
        doThrow(new ProdutoNaoEncontradoException(99L)).when(produtoService).remover(99L);

        mockMvc.perform(delete(URL + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve permitir requisições CORS do frontend local e dos deploys da Vercel")
    void devePermitirCorsDeOrigensConfiguradas() throws Exception {
        for (String origem : List.of("http://localhost:4200", "https://product-module-interface-abc123.vercel.app")) {
            mockMvc.perform(options(URL)
                            .header("Origin", origem)
                            .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origem));
        }
    }

    @Test
    @DisplayName("Deve recusar requisições CORS de origens não configuradas")
    void deveRecusarCorsDeOrigemNaoConfigurada() throws Exception {
        mockMvc.perform(options(URL)
                        .header("Origin", "https://site-desconhecido.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar 500 com mensagem genérica em erro inesperado")
    void deveRetornar500EmErroInesperado() throws Exception {
        when(produtoService.listarTodos()).thenThrow(new IllegalStateException("falha interna"));

        mockMvc.perform(get(URL))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."));
    }
}
