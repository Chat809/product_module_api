package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.config.SecurityConfig;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioDTO;
import br.com.triboraizes.produtosapi.exception.OperacaoNaoPermitidaException;
import br.com.triboraizes.produtosapi.model.Papel;
import br.com.triboraizes.produtosapi.service.UsuarioService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerTest {

    private static final String URL = "/api/v1/usuarios";
    private static final UUID ADMIN = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTRO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UsuarioDTO USUARIO = new UsuarioDTO(OUTRO, "Maria", "maria@exemplo.com", Papel.USUARIO,
            Instant.parse("2026-11-03T08:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static JwtRequestPostProcessor comoAdmin() {
        return jwt().jwt(token -> token.subject(ADMIN.toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static JwtRequestPostProcessor comoUsuario() {
        return jwt().jwt(token -> token.subject(OUTRO.toString())).authorities(new SimpleGrantedAuthority("ROLE_USUARIO"));
    }

    @Test
    @DisplayName("GET /me deve devolver o usuário do token, para qualquer papel")
    void deveDevolverUsuarioLogado() throws Exception {
        when(usuarioService.buscar(OUTRO)).thenReturn(USUARIO);

        mockMvc.perform(get(URL + "/me").with(comoUsuario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"))
                .andExpect(jsonPath("$.papel").value("USUARIO"));
    }

    @Test
    @DisplayName("Sem token deve responder 401 em ProblemDetail")
    void deveExigirToken() throws Exception {
        mockMvc.perform(get(URL + "/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }

    @Test
    @DisplayName("GET /usuarios deve responder 403 para quem não é ADMIN")
    void deveRestringirListagemAoAdmin() throws Exception {
        mockMvc.perform(get(URL).with(comoUsuario()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Acesso negado"));

        verifyNoInteractions(usuarioService);
    }

    @Test
    @DisplayName("GET /usuarios deve listar para ADMIN")
    void deveListarParaAdmin() throws Exception {
        when(usuarioService.listar(0, 20)).thenReturn(new PaginaDTO<>(List.of(USUARIO), 0, 20, 1, 1));

        mockMvc.perform(get(URL).with(comoAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo", hasSize(1)))
                .andExpect(jsonPath("$.conteudo[0].criadoEm").value("2026-11-03T08:00:00Z"));
    }

    @Test
    @DisplayName("PATCH /{id}/papel deve repassar o id do admin logado")
    void deveAlterarPapel() throws Exception {
        UsuarioDTO promovido = new UsuarioDTO(OUTRO, "Maria", "maria@exemplo.com", Papel.ADMIN, null);
        when(usuarioService.alterarPapel(OUTRO, Papel.ADMIN, ADMIN)).thenReturn(promovido);

        mockMvc.perform(patch(URL + "/" + OUTRO + "/papel").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"papel\": \"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ADMIN"));
    }

    @Test
    @DisplayName("PATCH /{id}/papel deve responder 422 quando o admin tentar se rebaixar")
    void deveRecusarAutoRebaixamento() throws Exception {
        when(usuarioService.alterarPapel(ADMIN, Papel.USUARIO, ADMIN))
                .thenThrow(new OperacaoNaoPermitidaException("Você não pode remover o seu próprio acesso de administrador"));

        mockMvc.perform(patch(URL + "/" + ADMIN + "/papel").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"papel\": \"USUARIO\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Operação não permitida"));
    }

    @Test
    @DisplayName("PATCH /{id}/papel deve validar o corpo e bloquear quem não é ADMIN")
    void deveValidarAlteracaoDePapel() throws Exception {
        mockMvc.perform(patch(URL + "/" + OUTRO + "/papel").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch(URL + "/" + OUTRO + "/papel").with(comoUsuario())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"papel\": \"ADMIN\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }
}
