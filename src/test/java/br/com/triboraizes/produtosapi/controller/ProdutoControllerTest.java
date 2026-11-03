package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoRequestDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoResponseDTO;
import br.com.triboraizes.produtosapi.exception.ConflitoDeVersaoException;
import br.com.triboraizes.produtosapi.exception.ProdutoNaoEncontradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.service.ProdutoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID INEXISTENTE = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final Instant AGORA = Instant.parse("2026-11-03T12:00:00Z");

    private static final ProdutoResponseDTO RESPONSE =
            new ProdutoResponseDTO(ID, "Teclado", "Mecânico", new BigDecimal("349.90"), 25, AGORA, AGORA, 0L);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProdutoService produtoService;

    @Test
    @DisplayName("GET /produtos deve retornar 200 com a primeira página ordenada por nome por padrão")
    void deveListarProdutos() throws Exception {
        when(produtoService.listar(null, 0, 20, "nome,asc"))
                .thenReturn(new PaginaDTO<>(List.of(RESPONSE), 0, 20, 1, 1));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].id").value(ID.toString()))
                .andExpect(jsonPath("$.conteudo[0].versao").value(0))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    @DisplayName("GET /produtos deve repassar busca, página, tamanho e ordenação ao serviço")
    void deveRepassarParametrosDeListagem() throws Exception {
        when(produtoService.listar("teclado", 2, 5, "preco,desc"))
                .thenReturn(new PaginaDTO<>(List.of(), 2, 5, 0, 0));

        mockMvc.perform(get(URL).param("busca", "teclado").param("page", "2").param("size", "5").param("sort", "preco,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(2));
    }

    @Test
    @DisplayName("GET /produtos deve retornar 400 quando um parâmetro de listagem for inválido")
    void deveRetornar400EmParametroDeListagemInvalido() throws Exception {
        when(produtoService.listar(null, 0, 1000, "nome,asc"))
                .thenThrow(new RequisicaoInvalidaException("O parâmetro 'size' deve estar entre 1 e 500"));

        mockMvc.perform(get(URL).param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
                .andExpect(jsonPath("$.detail").value("O parâmetro 'size' deve estar entre 1 e 500"));
    }

    @Test
    @DisplayName("GET /produtos/{id} deve retornar 200 com o produto")
    void deveBuscarProdutoPorId() throws Exception {
        when(produtoService.buscarPorId(ID)).thenReturn(RESPONSE);

        mockMvc.perform(get(URL + "/" + ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preco").value(349.90))
                .andExpect(jsonPath("$.quantidadeEmEstoque").value(25));
    }

    @Test
    @DisplayName("GET /produtos/{id} deve retornar 404 quando o produto não existir")
    void deveRetornar404QuandoProdutoNaoExistir() throws Exception {
        when(produtoService.buscarPorId(INEXISTENTE)).thenThrow(new ProdutoNaoEncontradoException(INEXISTENTE));

        mockMvc.perform(get(URL + "/" + INEXISTENTE))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Produto não encontrado"))
                .andExpect(jsonPath("$.detail").value("Produto com id " + INEXISTENTE + " não encontrado"));
    }

    @Test
    @DisplayName("GET /produtos/{id} deve retornar 400 quando o id não for um UUID válido")
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
                .andExpect(header().string("Location", "http://localhost/api/v1/produtos/" + ID))
                .andExpect(jsonPath("$.id").value(ID.toString()));
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
        when(produtoService.atualizar(eq(ID), any(ProdutoRequestDTO.class))).thenReturn(RESPONSE);

        mockMvc.perform(put(URL + "/" + ID).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Teclado"));
    }

    @Test
    @DisplayName("PUT /produtos/{id} deve retornar 409 quando a versão enviada estiver desatualizada")
    void deveRetornar409EmConflitoDeVersao() throws Exception {
        when(produtoService.atualizar(eq(ID), any(ProdutoRequestDTO.class)))
                .thenThrow(new ConflitoDeVersaoException(ID));

        mockMvc.perform(put(URL + "/" + ID).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de edição"));
    }

    @Test
    @DisplayName("PUT /produtos/{id} deve retornar 409 quando outra transação alterar o produto ao mesmo tempo")
    void deveRetornar409EmEdicaoSimultanea() throws Exception {
        when(produtoService.atualizar(eq(ID), any(ProdutoRequestDTO.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException("Produto", ID));

        mockMvc.perform(put(URL + "/" + ID).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /produtos/{id} deve retornar 404 quando o produto não existir")
    void deveRetornar404AoAtualizarProdutoInexistente() throws Exception {
        when(produtoService.atualizar(eq(INEXISTENTE), any(ProdutoRequestDTO.class)))
                .thenThrow(new ProdutoNaoEncontradoException(INEXISTENTE));

        mockMvc.perform(put(URL + "/" + INEXISTENTE).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /produtos/{id} deve retornar 204")
    void deveRemoverProduto() throws Exception {
        mockMvc.perform(delete(URL + "/" + ID))
                .andExpect(status().isNoContent());

        verify(produtoService).remover(ID);
    }

    @Test
    @DisplayName("DELETE /produtos/{id} deve retornar 404 quando o produto não existir")
    void deveRetornar404AoRemoverProdutoInexistente() throws Exception {
        doThrow(new ProdutoNaoEncontradoException(INEXISTENTE)).when(produtoService).remover(INEXISTENTE);

        mockMvc.perform(delete(URL + "/" + INEXISTENTE))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve permitir requisições CORS do frontend local e dos deploys da Vercel")
    void devePermitirCorsDeOrigensConfiguradas() throws Exception {
        for (String origem : List.of(
                "http://localhost:4200",
                "https://productmoduleinterface.vercel.app",
                "https://productmoduleinterface-git-main-chat809.vercel.app",
                "https://product-module-interface-abc123.vercel.app")) {
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
        when(produtoService.listar(null, 0, 20, "nome,asc")).thenThrow(new IllegalStateException("falha interna"));

        mockMvc.perform(get(URL))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."));
    }
}
