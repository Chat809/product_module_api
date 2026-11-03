package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.config.SecurityConfig;
import br.com.triboraizes.produtosapi.dto.ContaDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.ResumoFinanceiroDTO;
import br.com.triboraizes.produtosapi.exception.ContaNaoEncontradaException;
import br.com.triboraizes.produtosapi.model.FormaPagamento;
import br.com.triboraizes.produtosapi.model.StatusConta;
import br.com.triboraizes.produtosapi.model.TipoConta;
import br.com.triboraizes.produtosapi.service.ContaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FinanceiroController.class)
@Import(SecurityConfig.class)
class FinanceiroControllerTest {

    private static final String URL = "/api/v1/financeiro";
    private static final UUID ID = UUID.fromString("8c3e4d2a-6f5b-4c7d-8e9f-2a3b4c5d6e7f");
    private static final UUID MARIA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final ContaDTO CONTA = new ContaDTO(ID, "Aluguel", "Imobiliária", null, null,
            new BigDecimal("2500.00"), new BigDecimal("6.50"), new BigDecimal("162.50"), new BigDecimal("2337.50"),
            LocalDate.parse("2026-11-10"), null, StatusConta.PENDENTE, false, FormaPagamento.PIX, "pix@x.com",
            null, MARIA, null, null);
    private static final String CORPO = """
            {"descricao": "Aluguel", "pessoa": "Imobiliária", "valor": 2500.00, "percentualImposto": 6.5,
             "vencimento": "2026-11-10", "formaPagamento": "PIX", "chavePix": "pix@x.com"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContaService contaService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static JwtRequestPostProcessor comFinanceiro() {
        return jwt().jwt(token -> token.subject(MARIA.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_USUARIO"), new SimpleGrantedAuthority("MODULO_FINANCEIRO"));
    }

    @Test
    @DisplayName("Sem o módulo financeiro deve responder 403 em todas as rotas")
    void deveExigirModulo() throws Exception {
        JwtRequestPostProcessor semFinanceiro = jwt().authorities(new SimpleGrantedAuthority("MODULO_DEMANDAS"),
                new SimpleGrantedAuthority("MODULO_PRODUTOS"));

        mockMvc.perform(get(URL + "/resumo").with(semFinanceiro)).andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/contas-pagar").with(semFinanceiro)).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/contas-receber").with(semFinanceiro)
                .contentType(MediaType.APPLICATION_JSON).content(CORPO)).andExpect(status().isForbidden());
        verifyNoInteractions(contaService);
    }

    @Test
    @DisplayName("GET deve listar contas a pagar com filtros e datas ISO")
    void deveListar() throws Exception {
        when(contaService.listar(TipoConta.PAGAR, StatusConta.PENDENTE, true, "alu", LocalDate.parse("2026-11-01"),
                LocalDate.parse("2026-11-30"), 0, 10)).thenReturn(new PaginaDTO<>(List.of(CONTA), 0, 10, 1, 1));

        mockMvc.perform(get(URL + "/contas-pagar").with(comFinanceiro()).param("status", "PENDENTE")
                        .param("vencidas", "true").param("busca", "alu").param("vencimentoDe", "2026-11-01")
                        .param("vencimentoAte", "2026-11-30").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].valorLiquido").value(2337.50))
                .andExpect(jsonPath("$.conteudo[0].chavePix").value("pix@x.com"));
    }

    @Test
    @DisplayName("Tipo de conta fora de contas-pagar/contas-receber não existe")
    void deveRecusarTipoDesconhecido() throws Exception {
        mockMvc.perform(get(URL + "/contas-outras").with(comFinanceiro())).andExpect(status().isNotFound());
        verifyNoInteractions(contaService);
    }

    @Test
    @DisplayName("POST em contas-receber deve lançar com o usuário logado e responder 201")
    void deveCriar() throws Exception {
        when(contaService.criar(eq(TipoConta.RECEBER), any(), eq(MARIA))).thenReturn(CONTA);

        mockMvc.perform(post(URL + "/contas-receber").with(comFinanceiro())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valorImposto").value(162.50));
    }

    @Test
    @DisplayName("POST deve validar valor, imposto e campos obrigatórios")
    void deveValidar() throws Exception {
        mockMvc.perform(post(URL + "/contas-pagar").with(comFinanceiro()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"\", \"valor\": 0, \"percentualImposto\": 101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasSize(6)));
        verifyNoInteractions(contaService);
    }

    @Test
    @DisplayName("GET /{id}, PUT, baixa, cancelamento e DELETE repassam tipo e id")
    void deveOperarPorId() throws Exception {
        when(contaService.buscar(TipoConta.PAGAR, ID)).thenThrow(new ContaNaoEncontradaException(ID));
        when(contaService.atualizar(eq(TipoConta.PAGAR), eq(ID), any())).thenReturn(CONTA);
        when(contaService.baixar(TipoConta.PAGAR, ID, LocalDate.parse("2026-11-02"), MARIA)).thenReturn(CONTA);
        when(contaService.baixar(TipoConta.RECEBER, ID, null, MARIA)).thenReturn(CONTA);
        when(contaService.cancelar(TipoConta.PAGAR, ID, MARIA)).thenReturn(CONTA);

        mockMvc.perform(get(URL + "/contas-pagar/" + ID).with(comFinanceiro()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Conta não encontrada"));
        mockMvc.perform(put(URL + "/contas-pagar/" + ID).with(comFinanceiro())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk());
        mockMvc.perform(patch(URL + "/contas-pagar/" + ID + "/baixa").with(comFinanceiro())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"data\": \"2026-11-02\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch(URL + "/contas-receber/" + ID + "/baixa").with(comFinanceiro()))
                .andExpect(status().isOk());
        mockMvc.perform(patch(URL + "/contas-pagar/" + ID + "/cancelamento").with(comFinanceiro()))
                .andExpect(status().isOk());
        mockMvc.perform(delete(URL + "/contas-pagar/" + ID).with(comFinanceiro()))
                .andExpect(status().isNoContent());
        verify(contaService).excluir(TipoConta.PAGAR, ID, MARIA);
    }

    @Test
    @DisplayName("GET /resumo devolve os totais e o saldo previsto")
    void deveResumir() throws Exception {
        when(contaService.resumo()).thenReturn(new ResumoFinanceiroDTO(new BigDecimal("300"), BigDecimal.ZERO, 0,
                new BigDecimal("1000"), BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("700")));

        mockMvc.perform(get(URL + "/resumo").with(comFinanceiro()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aPagar").value(300))
                .andExpect(jsonPath("$.saldoPrevisto").value(700));
    }
}
