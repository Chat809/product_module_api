package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.config.SecurityConfig;
import br.com.triboraizes.produtosapi.dto.AtualizarUsuarioRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioOpcaoDTO;
import br.com.triboraizes.produtosapi.exception.EmailJaCadastradoException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
            List.of("dashboard", "produtos"), Instant.parse("2026-11-03T08:00:00Z"), null);
    private static final String EDICAO = """
            {"nome": "Maria", "email": "maria@exemplo.com", "papel": "USUARIO", "acessos": ["dashboard"]}
            """;

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
    @DisplayName("GET /opcoes deve liberar quem tem o módulo de demandas, mesmo sem ser ADMIN")
    void deveListarOpcoesParaQuemTemDemandas() throws Exception {
        when(usuarioService.opcoes()).thenReturn(List.of(new UsuarioOpcaoDTO(OUTRO, "Maria", null)));

        mockMvc.perform(get(URL + "/opcoes").with(jwt().jwt(token -> token.subject(OUTRO.toString()))
                        .authorities(new SimpleGrantedAuthority("ROLE_USUARIO"), new SimpleGrantedAuthority("MODULO_DEMANDAS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Maria"))
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    @DisplayName("GET /opcoes deve responder 403 sem o módulo de demandas")
    void deveBloquearOpcoesSemDemandas() throws Exception {
        mockMvc.perform(get(URL + "/opcoes").with(comoUsuario()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
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
    @DisplayName("PUT /{id} deve repassar os dados e o id do admin logado")
    void deveAtualizar() throws Exception {
        AtualizarUsuarioRequestDTO pedido = new AtualizarUsuarioRequestDTO(
                "Maria", "maria@exemplo.com", Papel.USUARIO, List.of("dashboard"));
        UsuarioDTO atualizado = new UsuarioDTO(OUTRO, "Maria", "maria@exemplo.com", Papel.USUARIO,
                List.of("dashboard"), null, null);
        when(usuarioService.atualizar(OUTRO, pedido, ADMIN)).thenReturn(atualizado);

        mockMvc.perform(put(URL + "/" + OUTRO).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(EDICAO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acessos[0]").value("dashboard"));
    }

    @Test
    @DisplayName("PUT /{id} deve responder 422 quando o admin tentar se rebaixar")
    void deveRecusarAutoRebaixamento() throws Exception {
        when(usuarioService.atualizar(eq(ADMIN), any(), eq(ADMIN)))
                .thenThrow(new OperacaoNaoPermitidaException("Você não pode remover o seu próprio acesso de administrador"));

        mockMvc.perform(put(URL + "/" + ADMIN).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(EDICAO))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.title").value("Operação não permitida"));
    }

    @Test
    @DisplayName("PUT /{id} deve responder 409 quando o novo e-mail já for de outra conta")
    void deveRecusarEmailDeOutraConta() throws Exception {
        when(usuarioService.atualizar(eq(OUTRO), any(), eq(ADMIN)))
                .thenThrow(new EmailJaCadastradoException("maria@exemplo.com"));

        mockMvc.perform(put(URL + "/" + OUTRO).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content(EDICAO))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PUT /{id} deve validar o corpo e bloquear quem não é ADMIN")
    void deveValidarEdicao() throws Exception {
        mockMvc.perform(put(URL + "/" + OUTRO).with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nome\": \"\", \"email\": \"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasSize(4)));
        mockMvc.perform(put(URL + "/" + OUTRO).with(comoUsuario())
                        .contentType(MediaType.APPLICATION_JSON).content(EDICAO))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    @Test
    @DisplayName("PUT /{id}/senha deve redefinir a senha e responder 204")
    void deveRedefinirSenha() throws Exception {
        mockMvc.perform(put(URL + "/" + OUTRO + "/senha").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"senha\": \"novaSenha1\"}"))
                .andExpect(status().isNoContent());

        verify(usuarioService).redefinirSenha(OUTRO, "novaSenha1", ADMIN);
    }

    @Test
    @DisplayName("PUT /{id}/senha deve validar o tamanho e bloquear quem não é ADMIN")
    void deveValidarSenha() throws Exception {
        mockMvc.perform(put(URL + "/" + OUTRO + "/senha").with(comoAdmin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"senha\": \"123\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(URL + "/" + OUTRO + "/senha").with(comoUsuario())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"senha\": \"novaSenha1\"}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }
}
