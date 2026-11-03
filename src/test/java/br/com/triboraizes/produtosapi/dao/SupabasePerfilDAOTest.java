package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.config.SupabaseConfig;
import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.ServicoDeUsuariosIndisponivelException;
import br.com.triboraizes.produtosapi.model.Papel;
import br.com.triboraizes.produtosapi.model.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SupabasePerfilDAOTest {

    private static final String URL = "https://projeto.supabase.co";
    private static final String TABELA = URL + "/rest/v1/perfis";
    private static final UUID ID = UUID.fromString("6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e");
    private static final String PERFIL_JSON = """
            [{"id": "6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e", "nome": "Maria", "email": "maria@exemplo.com",
              "papel": "ADMIN", "criado_em": "2026-11-03T08:15:30.123456+00:00"}]
            """;

    private MockRestServiceServer servidor;
    private SupabasePerfilDAO dao;

    @BeforeEach
    void configurar() {
        SupabaseProperties properties = new SupabaseProperties(URL, "sb_secret_teste");
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        dao = new SupabasePerfilDAO(SupabaseConfig.criarRestClient(builder, properties), properties);
    }

    @Test
    @DisplayName("Deve buscar o perfil por id e converter o timestamp do Postgres")
    void deveBuscarPorId() {
        servidor.expect(requestTo(TABELA + "?select=id,nome,email,papel,criado_em&id=eq." + ID))
                .andExpect(method(GET))
                .andExpect(header("apikey", "sb_secret_teste"))
                .andRespond(withSuccess(PERFIL_JSON, MediaType.APPLICATION_JSON));

        Perfil perfil = dao.buscarPorId(ID).orElseThrow();

        assertThat(perfil.nome()).isEqualTo("Maria");
        assertThat(perfil.papel()).isEqualTo(Papel.ADMIN);
        assertThat(perfil.criadoEm()).isEqualTo(OffsetDateTime.parse("2026-11-03T08:15:30.123456Z"));
    }

    @Test
    @DisplayName("Deve devolver vazio quando o id não existir")
    void deveDevolverVazio() {
        servidor.expect(requestTo(TABELA + "?select=id,nome,email,papel,criado_em&id=eq." + ID))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(dao.buscarPorId(ID)).isEmpty();
    }

    @Test
    @DisplayName("Deve listar a página pedida em ordem alfabética")
    void deveListar() {
        servidor.expect(requestTo(TABELA + "?select=id,nome,email,papel,criado_em&order=nome.asc&offset=20&limit=10"))
                .andExpect(method(GET))
                .andRespond(withSuccess(PERFIL_JSON, MediaType.APPLICATION_JSON));

        List<Perfil> perfis = dao.listar(2, 10);

        assertThat(perfis).extracting(Perfil::email).containsExactly("maria@exemplo.com");
    }

    @Test
    @DisplayName("Deve contar os perfis pelo cabeçalho Content-Range")
    void deveContar() {
        HttpHeaders comTotal = new HttpHeaders();
        comTotal.add("Content-Range", "0-1/2");
        servidor.expect(requestTo(TABELA + "?select=id"))
                .andExpect(method(HEAD))
                .andExpect(header("Prefer", "count=exact"))
                .andRespond(withSuccess().headers(comTotal));
        servidor.expect(requestTo(TABELA + "?select=id"))
                .andRespond(withSuccess());

        assertThat(dao.contar()).isEqualTo(2);
        assertThat(dao.contar()).isZero();
    }

    @Test
    @DisplayName("Deve atualizar o papel e devolver o perfil gravado")
    void deveAtualizarPapel() {
        servidor.expect(requestTo(TABELA + "?id=eq." + ID + "&select=id,nome,email,papel,criado_em"))
                .andExpect(method(PATCH))
                .andExpect(header("Prefer", "return=representation"))
                .andExpect(content().json("{\"papel\": \"ADMIN\"}"))
                .andRespond(withSuccess(PERFIL_JSON, MediaType.APPLICATION_JSON));

        assertThat(dao.atualizarPapel(ID, Papel.ADMIN)).get().extracting(Perfil::papel).isEqualTo(Papel.ADMIN);
    }

    @Test
    @DisplayName("Falha no Supabase ou chave ausente viram serviço indisponível")
    void deveTratarFalhas() {
        servidor.expect(requestTo(TABELA + "?select=id")).andRespond(withServerError());
        assertThatThrownBy(() -> dao.contar()).isInstanceOf(ServicoDeUsuariosIndisponivelException.class);

        SupabaseProperties semChave = new SupabaseProperties(URL, null);
        SupabasePerfilDAO semConfiguracao = new SupabasePerfilDAO(
                SupabaseConfig.criarRestClient(RestClient.builder(), semChave), semChave);
        assertThatThrownBy(() -> semConfiguracao.listar(0, 10))
                .isInstanceOf(ServicoDeUsuariosIndisponivelException.class)
                .hasMessageContaining("SUPABASE_SECRET_KEY");
    }
}
