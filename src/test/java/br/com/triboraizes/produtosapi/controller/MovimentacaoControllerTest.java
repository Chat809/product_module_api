package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.MovimentacaoRequestDTO;
import br.com.triboraizes.produtosapi.dto.MovimentacaoResponseDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.exception.EstoqueInsuficienteException;
import br.com.triboraizes.produtosapi.model.TipoMovimentacao;
import br.com.triboraizes.produtosapi.service.MovimentacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MovimentacaoController.class)
class MovimentacaoControllerTest {

    private static final UUID PRODUTO_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String URL = "/api/v1/produtos/" + PRODUTO_ID + "/movimentacoes";

    private static final MovimentacaoResponseDTO RESPONSE = new MovimentacaoResponseDTO(
            UUID.fromString("22222222-2222-2222-2222-222222222222"), PRODUTO_ID, TipoMovimentacao.ENTRADA,
            5, 10, 15, "Reposição", Instant.parse("2026-11-03T12:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovimentacaoService movimentacaoService;

    @Test
    @DisplayName("POST /movimentacoes deve retornar 201 com a movimentação registrada")
    void deveRegistrarMovimentacao() throws Exception {
        when(movimentacaoService.registrar(eq(PRODUTO_ID), any(MovimentacaoRequestDTO.class))).thenReturn(RESPONSE);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "ENTRADA", "quantidade": 5, "motivo": "Reposição"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.saldoApos").value(15));
    }

    @Test
    @DisplayName("POST /movimentacoes deve retornar 400 quando tipo e quantidade forem inválidos")
    void deveRetornar400ComDadosInvalidos() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{\"quantidade\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasSize(2)));

        verifyNoInteractions(movimentacaoService);
    }

    @Test
    @DisplayName("POST /movimentacoes deve retornar 422 quando a saída for maior que o estoque")
    void deveRetornar422EmEstoqueInsuficiente() throws Exception {
        when(movimentacaoService.registrar(eq(PRODUTO_ID), any(MovimentacaoRequestDTO.class)))
                .thenThrow(new EstoqueInsuficienteException(3, 5));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\": \"SAIDA\", \"quantidade\": 5}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Estoque insuficiente"));
    }

    @Test
    @DisplayName("GET /movimentacoes deve retornar 200 com o histórico paginado")
    void deveListarHistorico() throws Exception {
        when(movimentacaoService.listar(PRODUTO_ID, 0, 20)).thenReturn(new PaginaDTO<>(List.of(RESPONSE), 0, 20, 1, 1));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].motivo").value("Reposição"));
    }
}
