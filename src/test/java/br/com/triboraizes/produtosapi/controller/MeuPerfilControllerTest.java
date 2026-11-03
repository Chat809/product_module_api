package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.config.SecurityConfig;
import br.com.triboraizes.produtosapi.dto.AtualizarMeuPerfilRequestDTO;
import br.com.triboraizes.produtosapi.dto.MeuPerfilDTO;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.model.Papel;
import br.com.triboraizes.produtosapi.service.MeuPerfilService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeuPerfilController.class)
@Import(SecurityConfig.class)
class MeuPerfilControllerTest {

    private static final String URL = "/api/v1/usuarios/me";
    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final MeuPerfilDTO PERFIL = new MeuPerfilDTO(ID, "Maria", "maria@exemplo.com", Papel.USUARIO,
            List.of("dashboard"), null, "(11) 9999-0000", "Compradora", null, "https://x/a.png", "Compras");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MeuPerfilService meuPerfilService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    /** Usuário sem nenhum módulo: o próprio perfil continua acessível. */
    private static JwtRequestPostProcessor semModulos() {
        return jwt().jwt(token -> token.subject(ID.toString())).authorities(new SimpleGrantedAuthority("ROLE_USUARIO"));
    }

    @Test
    @DisplayName("GET deve devolver o perfil do token, mesmo sem nenhum módulo")
    void deveBuscar() throws Exception {
        when(meuPerfilService.buscar(ID)).thenReturn(PERFIL);

        mockMvc.perform(get(URL).with(semModulos()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefone").value("(11) 9999-0000"))
                .andExpect(jsonPath("$.avatarUrl").value("https://x/a.png"));
    }

    @Test
    @DisplayName("Sem token deve responder 401")
    void deveExigirToken() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(multipart(HttpMethod.PUT, URL + "/avatar")
                        .file(new MockMultipartFile("arquivo", "a.png", "image/png", new byte[]{1})))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(meuPerfilService);
    }

    @Test
    @DisplayName("PUT deve repassar os dados e ignorar e-mail, papel, acessos, cargo e departamento enviados")
    void deveAtualizar() throws Exception {
        when(meuPerfilService.atualizar(ID, new AtualizarMeuPerfilRequestDTO("Maria", "(11) 9999-0000", null)))
                .thenReturn(PERFIL);

        mockMvc.perform(put(URL).with(semModulos()).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome": "Maria", "telefone": "(11) 9999-0000", "funcao": "Diretora", "departamento": "Diretoria",
                         "email": "outro@exemplo.com", "papel": "ADMIN", "acessos": ["*"]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("maria@exemplo.com"))
                .andExpect(jsonPath("$.papel").value("USUARIO"))
                .andExpect(jsonPath("$.funcao").value("Compradora"))
                .andExpect(jsonPath("$.departamento").value("Compras"));
    }

    @Test
    @DisplayName("PUT deve validar nome, telefone e tamanhos")
    void deveValidar() throws Exception {
        mockMvc.perform(put(URL).with(semModulos()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"\", \"telefone\": \"abc\", \"bio\": \"" + "x".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros", hasSize(3)));
        verifyNoInteractions(meuPerfilService);
    }

    @Test
    @DisplayName("PUT /avatar deve enviar os bytes do arquivo")
    void deveTrocarAvatar() throws Exception {
        byte[] conteudo = {(byte) 0x89, 'P', 'N', 'G'};
        when(meuPerfilService.trocarAvatar(ID, conteudo)).thenReturn(PERFIL);

        mockMvc.perform(multipart(HttpMethod.PUT, URL + "/avatar")
                        .file(new MockMultipartFile("arquivo", "a.png", "image/png", conteudo)).with(semModulos()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("https://x/a.png"));
    }

    @Test
    @DisplayName("PUT /avatar deve responder 400 para formato inválido ou campo ausente")
    void deveRecusarAvatarInvalido() throws Exception {
        when(meuPerfilService.trocarAvatar(eq(ID), any()))
                .thenThrow(new RequisicaoInvalidaException("Formato não suportado. Envie uma imagem PNG, JPEG ou WEBP"));

        mockMvc.perform(multipart(HttpMethod.PUT, URL + "/avatar")
                        .file(new MockMultipartFile("arquivo", "a.gif", "image/gif", new byte[]{1})).with(semModulos()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Formato não suportado. Envie uma imagem PNG, JPEG ou WEBP"));
        mockMvc.perform(multipart(HttpMethod.PUT, URL + "/avatar")
                        .file(new MockMultipartFile("outro", "a.png", "image/png", new byte[]{1})).with(semModulos()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /avatar deve remover o avatar do usuário logado")
    void deveRemoverAvatar() throws Exception {
        when(meuPerfilService.removerAvatar(ID)).thenReturn(PERFIL);

        mockMvc.perform(delete(URL + "/avatar").with(semModulos())).andExpect(status().isOk());
    }
}
