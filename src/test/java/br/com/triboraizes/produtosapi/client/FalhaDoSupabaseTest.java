package br.com.triboraizes.produtosapi.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FalhaDoSupabaseTest {

    @Test
    @DisplayName("Deve descrever a falha só pelo status, sem o corpo da resposta")
    void deveDescreverSoPeloStatus() {
        HttpClientErrorException erro = HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null,
                "{\"msg\":\"maria@exemplo.com já existe\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        assertThat(FalhaDoSupabase.descrever(erro)).isEqualTo("HTTP 400");
    }

    @Test
    @DisplayName("Deve descrever falhas de conexão só pelo tipo, sem a URL chamada")
    void deveDescreverFalhaDeConexaoPeloTipo() {
        ResourceAccessException erro = new ResourceAccessException(
                "I/O error on GET request for \"https://x.supabase.co/rest/v1/perfis?email=eq.maria@exemplo.com\"");

        assertThat(FalhaDoSupabase.descrever(erro)).isEqualTo("ResourceAccessException");
    }
}
