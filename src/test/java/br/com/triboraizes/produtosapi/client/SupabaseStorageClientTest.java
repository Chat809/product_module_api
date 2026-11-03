package br.com.triboraizes.produtosapi.client;

import br.com.triboraizes.produtosapi.config.SupabaseConfig;
import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.ServicoDeDadosIndisponivelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabaseStorageClientTest {

    private static final String URL = "https://projeto.supabase.co";
    private static final String PUBLICO = URL + "/storage/v1/object/public/avatars/";

    private MockRestServiceServer servidor;
    private SupabaseStorageClient client;

    @BeforeEach
    void configurar() {
        SupabaseProperties properties = new SupabaseProperties(URL, "sb_secret_teste");
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        client = new SupabaseStorageClient(SupabaseConfig.criarRestClient(builder, properties), properties);
    }

    @Test
    @DisplayName("Deve enviar o arquivo com o tipo detectado, sem sobrescrever, e devolver a URL pública")
    void deveEnviar() {
        servidor.expect(requestTo(URL + "/storage/v1/object/avatars/u1/a.png"))
                .andExpect(method(POST))
                .andExpect(header("apikey", "sb_secret_teste"))
                .andExpect(header("x-upsert", "false"))
                .andExpect(header("Content-Type", "image/png"))
                .andRespond(withSuccess("{\"Key\": \"avatars/u1/a.png\"}", MediaType.APPLICATION_JSON));

        String url = client.enviar("avatars", "u1", "a.png", new byte[]{1, 2}, "image/png");

        assertThat(url).isEqualTo(PUBLICO + "u1/a.png");
    }

    @Test
    @DisplayName("Falha no envio ou chave ausente viram serviço indisponível")
    void deveTratarFalhaNoEnvio() {
        servidor.expect(requestTo(URL + "/storage/v1/object/avatars/u1/a.png")).andRespond(withServerError());
        assertThatThrownBy(() -> client.enviar("avatars", "u1", "a.png", new byte[]{1}, "image/png"))
                .isInstanceOf(ServicoDeDadosIndisponivelException.class);

        SupabaseProperties semChave = new SupabaseProperties(URL, null);
        SupabaseStorageClient semConfiguracao = new SupabaseStorageClient(
                SupabaseConfig.criarRestClient(RestClient.builder(), semChave), semChave);
        assertThatThrownBy(() -> semConfiguracao.enviar("avatars", "u1", "a.png", new byte[]{1}, "image/png"))
                .hasMessageContaining("SUPABASE_SECRET_KEY");
    }

    @Test
    @DisplayName("Deve remover pelo prefixo e não interromper a operação se o Storage falhar")
    void deveRemover() {
        servidor.expect(requestTo(URL + "/storage/v1/object/avatars"))
                .andExpect(method(DELETE))
                .andExpect(content().json("{\"prefixes\": [\"u1/a.png\"]}", JsonCompareMode.STRICT))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL + "/storage/v1/object/avatars")).andRespond(withServerError());

        client.remover("avatars", "u1/a.png");
        assertThatCode(() -> client.remover("avatars", "u1/a.png")).doesNotThrowAnyException();
        servidor.verify();
    }

    @Test
    @DisplayName("Só reconhece caminhos de URLs do próprio bucket")
    void deveExtrairCaminho() {
        assertThat(client.caminhoDe("avatars", PUBLICO + "u1/a.png")).contains("u1/a.png");
        assertThat(client.caminhoDe("avatars", "https://outro.site/a.png")).isEmpty();
        assertThat(client.caminhoDe("avatars", null)).isEmpty();
    }
}
