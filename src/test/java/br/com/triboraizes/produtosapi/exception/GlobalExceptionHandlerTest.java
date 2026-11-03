package br.com.triboraizes.produtosapi.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica o formato RFC 9457 de cada erro tratado, isolando o handler com um controller
 * de teste que apenas lança as exceções.
 */
class GlobalExceptionHandlerTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    record Entrada(@NotBlank(message = "O nome é obrigatório") String nome,
                   @Positive(message = "A quantidade deve ser positiva") Integer quantidade) {
    }

    @RestController
    static class ControllerDeTeste {

        @GetMapping("/nao-encontrado")
        void naoEncontrado() {
            throw new ProdutoNaoEncontradoException(ID);
        }

        @GetMapping("/conflito")
        void conflito() {
            throw new ConflitoDeVersaoException(ID);
        }

        @GetMapping("/edicao-simultanea")
        void edicaoSimultanea() {
            throw new ObjectOptimisticLockingFailureException("Produto", ID);
        }

        @GetMapping("/estoque-insuficiente")
        void estoqueInsuficiente() {
            throw new EstoqueInsuficienteException(2, 5);
        }

        @GetMapping("/parametro-invalido")
        void parametroInvalido() {
            throw new RequisicaoInvalidaException("O parâmetro 'size' deve estar entre 1 e 500");
        }

        @GetMapping("/uuid/{id}")
        void uuid(@PathVariable UUID id) {
        }

        @PostMapping("/validacao")
        void validacao(@Valid @RequestBody Entrada entrada) {
        }

        @GetMapping("/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalhe interno que não pode vazar");
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ControllerDeTeste())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validador)
                .build();
    }

    private ResultActions problema(ResultActions resultado, int status, String titulo) throws Exception {
        return resultado
                .andExpect(status().is(status))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.title").value(titulo));
    }

    @Test
    @DisplayName("404 com título e detalhe do produto não encontrado")
    void deveTratarProdutoNaoEncontrado() throws Exception {
        problema(mockMvc.perform(get("/nao-encontrado")), 404, "Produto não encontrado")
                .andExpect(jsonPath("$.detail").value("Produto com id " + ID + " não encontrado"))
                .andExpect(jsonPath("$.instance").value("/nao-encontrado"));
    }

    @Test
    @DisplayName("409 para versão desatualizada e para edição simultânea")
    void deveTratarConflitos() throws Exception {
        problema(mockMvc.perform(get("/conflito")), 409, "Conflito de edição")
                .andExpect(jsonPath("$.detail").value(
                        "O produto " + ID + " foi alterado por outra pessoa. Recarregue os dados e tente novamente."));
        problema(mockMvc.perform(get("/edicao-simultanea")), 409, "Conflito de edição")
                .andExpect(jsonPath("$.detail").value(
                        "O produto foi alterado por outra pessoa. Recarregue os dados e tente novamente."));
    }

    @Test
    @DisplayName("422 para saída maior que o estoque")
    void deveTratarEstoqueInsuficiente() throws Exception {
        problema(mockMvc.perform(get("/estoque-insuficiente")), 422, "Estoque insuficiente")
                .andExpect(jsonPath("$.detail").value(
                        "Estoque insuficiente: há 2 unidade(s) disponível(is) e a saída pedida é de 5"));
    }

    @Test
    @DisplayName("400 para parâmetro inválido e para UUID malformado")
    void deveTratarParametrosInvalidos() throws Exception {
        problema(mockMvc.perform(get("/parametro-invalido")), 400, "Parâmetro inválido")
                .andExpect(jsonPath("$.detail").value("O parâmetro 'size' deve estar entre 1 e 500"));
        problema(mockMvc.perform(get("/uuid/abc")), 400, "Parâmetro inválido")
                .andExpect(jsonPath("$.detail").value("O parâmetro 'id' possui um valor inválido"));
    }

    @Test
    @DisplayName("400 com a lista de campos inválidos")
    void deveTratarCamposInvalidos() throws Exception {
        problema(mockMvc.perform(post("/validacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"\", \"quantidade\": 0}")), 400, "Dados inválidos")
                .andExpect(jsonPath("$.detail").value("Um ou mais campos são inválidos"))
                .andExpect(jsonPath("$.erros", hasSize(2)))
                .andExpect(jsonPath("$.erros[?(@.campo == 'nome')].mensagem").value("O nome é obrigatório"))
                .andExpect(jsonPath("$.erros[?(@.campo == 'quantidade')].mensagem")
                        .value("A quantidade deve ser positiva"));
    }

    @Test
    @DisplayName("400 para corpo ausente ou malformado")
    void deveTratarCorpoMalformado() throws Exception {
        problema(mockMvc.perform(post("/validacao").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":")),
                400, "Requisição inválida")
                .andExpect(jsonPath("$.detail").value("O corpo da requisição está ausente ou malformado"));
    }

    @Test
    @DisplayName("500 com mensagem genérica, sem expor detalhes internos")
    void deveOcultarDetalhesDeErroInesperado() throws Exception {
        problema(mockMvc.perform(get("/inesperado")), 500, "Erro interno")
                .andExpect(jsonPath("$.detail").value("Ocorreu um erro inesperado. Tente novamente mais tarde."));
    }
}
