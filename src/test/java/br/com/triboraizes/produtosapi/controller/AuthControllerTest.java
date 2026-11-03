package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.config.SecurityConfig;
import br.com.triboraizes.produtosapi.dto.LoginRequestDTO;
import br.com.triboraizes.produtosapi.dto.RegistroRequestDTO;
import br.com.triboraizes.produtosapi.dto.SessaoDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioDTO;
import br.com.triboraizes.produtosapi.exception.CredenciaisInvalidasException;
import br.com.triboraizes.produtosapi.exception.EmailJaCadastradoException;
import br.com.triboraizes.produtosapi.exception.ServicoDeUsuariosIndisponivelException;
import br.com.triboraizes.produtosapi.model.Papel;
import br.com.triboraizes.produtosapi.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String URL = "/api/v1/auth";

    private static final SessaoDTO SESSAO = new SessaoDTO("acesso", "renovacao", Instant.parse("2026-11-03T13:00:00Z"),
            new UsuarioDTO(UUID.fromString("6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e"), "Maria", "maria@exemplo.com",
                    Papel.USUARIO, null));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("POST /registro deve criar a conta sem token e devolver 201 com a sessão")
    void deveRegistrar() throws Exception {
        when(authService.registrar(any(RegistroRequestDTO.class))).thenReturn(SESSAO);

        mockMvc.perform(post(URL + "/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Maria\", \"email\": \"maria@exemplo.com\", \"senha\": \"segredo1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("acesso"))
                .andExpect(jsonPath("$.usuario.papel").value("USUARIO"));
    }

    @Test
    @DisplayName("POST /registro deve validar nome, e-mail e tamanho da senha")
    void deveValidarRegistro() throws Exception {
        mockMvc.perform(post(URL + "/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"\", \"email\": \"nao-e-email\", \"senha\": \"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasSize(3)));

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("POST /registro deve devolver 409 para e-mail já cadastrado")
    void deveRecusarEmailRepetido() throws Exception {
        when(authService.registrar(any(RegistroRequestDTO.class)))
                .thenThrow(new EmailJaCadastradoException("maria@exemplo.com"));

        mockMvc.perform(post(URL + "/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Maria\", \"email\": \"maria@exemplo.com\", \"senha\": \"segredo1\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("E-mail já cadastrado"));
    }

    @Test
    @DisplayName("POST /login deve devolver a sessão")
    void deveEntrar() throws Exception {
        when(authService.entrar(any(LoginRequestDTO.class))).thenReturn(SESSAO);

        mockMvc.perform(post(URL + "/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"maria@exemplo.com\", \"senha\": \"segredo1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("renovacao"))
                .andExpect(jsonPath("$.expiraEm").value("2026-11-03T13:00:00Z"));
    }

    @Test
    @DisplayName("POST /login deve devolver 401 com credenciais erradas")
    void deveRecusarCredenciais() throws Exception {
        when(authService.entrar(any(LoginRequestDTO.class)))
                .thenThrow(new CredenciaisInvalidasException("E-mail ou senha inválidos"));

        mockMvc.perform(post(URL + "/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"maria@exemplo.com\", \"senha\": \"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciais inválidas"))
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos"));
    }

    @Test
    @DisplayName("POST /login deve devolver 503 quando o Supabase estiver fora")
    void deveInformarIndisponibilidade() throws Exception {
        when(authService.entrar(any(LoginRequestDTO.class)))
                .thenThrow(new ServicoDeUsuariosIndisponivelException("Serviço de usuários indisponível. Tente novamente.", null));

        mockMvc.perform(post(URL + "/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"maria@exemplo.com\", \"senha\": \"segredo1\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Serviço de usuários indisponível"));
    }

    @Test
    @DisplayName("POST /refresh deve renovar a sessão e exigir o refreshToken")
    void deveRenovar() throws Exception {
        when(authService.renovar("renovacao")).thenReturn(SESSAO);

        mockMvc.perform(post(URL + "/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\": \"renovacao\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acesso"));
        mockMvc.perform(post(URL + "/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
