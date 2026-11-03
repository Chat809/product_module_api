package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.config.SecurityConfig;
import br.com.triboraizes.produtosapi.dto.DemandaDTO;
import br.com.triboraizes.produtosapi.dto.MoverDemandaRequestDTO;
import br.com.triboraizes.produtosapi.exception.DemandaNaoEncontradaException;
import br.com.triboraizes.produtosapi.exception.OperacaoNaoPermitidaException;
import br.com.triboraizes.produtosapi.exception.ServicoDeDadosIndisponivelException;
import br.com.triboraizes.produtosapi.model.FiltroDemandas;
import br.com.triboraizes.produtosapi.model.StatusDemanda;
import br.com.triboraizes.produtosapi.model.TipoDemanda;
import br.com.triboraizes.produtosapi.model.UrgenciaDemanda;
import br.com.triboraizes.produtosapi.service.DemandaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

@WebMvcTest(DemandaController.class)
@Import(SecurityConfig.class)
class DemandaControllerTest {

    private static final String URL = "/api/v1/demandas";
    private static final UUID ID = UUID.fromString("7a2d3c1b-5e4f-4a6b-9c8d-1e2f3a4b5c6d");
    private static final UUID MARIA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final DemandaDTO DEMANDA = new DemandaDTO(ID, "Relatório", null, StatusDemanda.A_FAZER,
            UrgenciaDemanda.ALTA, TipoDemanda.MELHORIA, LocalDate.parse("2026-11-20"), false, List.of(MARIA),
            List.of(), 0, MARIA, null, null);
    private static final String CORPO = """
            {"titulo": "Relatório", "tipo": "MELHORIA", "urgencia": "ALTA",
             "responsaveis": ["22222222-2222-2222-2222-222222222222"], "checklist": [{"texto": "Revisar", "feito": false}]}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DemandaService demandaService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static JwtRequestPostProcessor comDemandas(String... papeis) {
        List<GrantedAuthority> autoridades = new ArrayList<>(List.of(new SimpleGrantedAuthority("MODULO_DEMANDAS")));
        for (String papel : papeis) {
            autoridades.add(new SimpleGrantedAuthority(papel));
        }
        return jwt().jwt(token -> token.subject(MARIA.toString())).authorities(autoridades);
    }

    @Test
    @DisplayName("GET com responsavel=me deve filtrar pelas tarefas do usuário logado")
    void deveListarMinhasTarefas() throws Exception {
        when(demandaService.listar(new FiltroDemandas(MARIA, null, null, true))).thenReturn(List.of(DEMANDA));

        mockMvc.perform(get(URL).param("responsavel", "me").param("abertas", "true").with(comDemandas()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].prazo").value("2026-11-20"));
    }

    @Test
    @DisplayName("GET deve aceitar um id em responsavel e recusar outros textos")
    void deveValidarResponsavel() throws Exception {
        when(demandaService.listar(new FiltroDemandas(ID, StatusDemanda.CONCLUIDO, "rel", false))).thenReturn(List.of());

        mockMvc.perform(get(URL).param("responsavel", ID.toString()).param("status", "CONCLUIDO").param("busca", "rel")
                        .with(comDemandas()))
                .andExpect(status().isOk());
        mockMvc.perform(get(URL).param("responsavel", "todos").with(comDemandas()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).param("status", "PARADO").with(comDemandas()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sem o módulo de demandas deve responder 403")
    void deveExigirModulo() throws Exception {
        mockMvc.perform(get(URL).with(jwt().authorities(new SimpleGrantedAuthority("MODULO_PRODUTOS"))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(demandaService);
    }

    @Test
    @DisplayName("POST deve criar com o usuário logado como autor e responder 201")
    void deveCriar() throws Exception {
        when(demandaService.criar(any(), eq(MARIA))).thenReturn(DEMANDA);

        mockMvc.perform(post(URL).with(comDemandas()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test
    @DisplayName("POST deve validar título, tipo, urgência e itens do checklist")
    void deveValidarCriacao() throws Exception {
        mockMvc.perform(post(URL).with(comDemandas()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\": \"\", \"checklist\": [{\"texto\": \"\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasSize(4)));
        verifyNoInteractions(demandaService);
    }

    @Test
    @DisplayName("GET /{id}, PUT e PATCH /status devolvem a demanda; id inexistente vira 404")
    void deveBuscarEditarEMover() throws Exception {
        when(demandaService.buscar(ID)).thenThrow(new DemandaNaoEncontradaException(ID));
        when(demandaService.atualizar(eq(ID), any())).thenReturn(DEMANDA);
        when(demandaService.mover(ID, new MoverDemandaRequestDTO(StatusDemanda.EM_ANDAMENTO, 0))).thenReturn(DEMANDA);

        mockMvc.perform(get(URL + "/" + ID).with(comDemandas()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Demanda não encontrada"));
        mockMvc.perform(put(URL + "/" + ID).with(comDemandas()).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isOk());
        mockMvc.perform(patch(URL + "/" + ID + "/status").with(comDemandas()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"EM_ANDAMENTO\", \"posicao\": 0}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch(URL + "/" + ID + "/status").with(comDemandas()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"posicao\": -1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasSize(2)));
    }

    @Test
    @DisplayName("DELETE deve informar se o usuário é admin; recusa vira 422")
    void deveExcluir() throws Exception {
        mockMvc.perform(delete(URL + "/" + ID).with(comDemandas("ROLE_ADMIN")))
                .andExpect(status().isNoContent());
        verify(demandaService).excluir(ID, MARIA, true);

        doThrow(new OperacaoNaoPermitidaException("Só quem criou a demanda ou um administrador pode excluí-la"))
                .when(demandaService).excluir(ID, MARIA, false);
        mockMvc.perform(delete(URL + "/" + ID).with(comDemandas("ROLE_USUARIO")))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    @DisplayName("Banco fora do ar vira 503")
    void deveResponder503() throws Exception {
        when(demandaService.listar(any())).thenThrow(new ServicoDeDadosIndisponivelException("Serviço de dados indisponível. Tente novamente.", null));

        mockMvc.perform(get(URL).with(comDemandas()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Serviço de dados indisponível"));
    }
}
