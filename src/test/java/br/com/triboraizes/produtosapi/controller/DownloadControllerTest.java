package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DownloadController.class)
@Import(SecurityConfig.class)
class DownloadControllerTest {

    private static final String URL = "/api/v1/download";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("GET sem token entrega o arquivo da pasta download como anexo")
    void baixaSemAutenticacao() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment;")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("exemplo.txt")))
                .andExpect(content().string("conteudo de teste"));
    }

    @Test
    @DisplayName("Só o GET é público; outros métodos continuam exigindo token")
    void outrosMetodosExigemToken() throws Exception {
        mockMvc.perform(post(URL)).andExpect(status().isUnauthorized());
    }
}
