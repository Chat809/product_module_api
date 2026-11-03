package br.com.triboraizes.produtosapi.client;

import br.com.triboraizes.produtosapi.client.SupabaseAuthClient.SessaoAuth;
import br.com.triboraizes.produtosapi.config.SupabaseConfig;
import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.CredenciaisInvalidasException;
import br.com.triboraizes.produtosapi.exception.EmailJaCadastradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.exception.ServicoDeUsuariosIndisponivelException;
import br.com.triboraizes.produtosapi.exception.UsuarioNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

class SupabaseAuthClientTest {

    private static final String URL = "https://projeto.supabase.co";
    private static final String CHAVE = "sb_secret_teste";
    private static final UUID ID = UUID.fromString("6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e");

    private static final String SESSAO = """
            {"access_token": "token-acesso", "token_type": "bearer", "expires_in": 3600,
             "expires_at": 1793707200, "refresh_token": "token-renovacao",
             "user": {"id": "6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e", "email": "maria@exemplo.com",
                      "app_metadata": {"provider": "email", "papel": "ADMIN"},
                      "user_metadata": {"nome": "Maria"}}}
            """;

    private MockRestServiceServer servidor;
    private SupabaseAuthClient client;

    @BeforeEach
    void configurar() {
        SupabaseProperties properties = new SupabaseProperties(URL, CHAVE);
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        client = new SupabaseAuthClient(SupabaseConfig.criarRestClient(builder, properties), properties);
    }

    @Test
    @DisplayName("Deve criar o usuário com e-mail confirmado e nome nos metadados, enviando só o apikey")
    void deveCriarUsuario() {
        servidor.expect(requestTo(URL + "/auth/v1/admin/users"))
                .andExpect(method(POST))
                .andExpect(header("apikey", CHAVE))
                .andExpect(headerDoesNotExist("Authorization"))
                .andExpect(content().json("""
                        {"email": "maria@exemplo.com", "password": "segredo1", "email_confirm": true,
                         "user_metadata": {"nome": "Maria"}}
                        """))
                .andRespond(withSuccess("{\"id\": \"" + ID + "\"}", MediaType.APPLICATION_JSON));

        client.criarUsuario("Maria", "maria@exemplo.com", "segredo1");

        servidor.verify();
    }

    @Test
    @DisplayName("Chave legada (JWT service_role) também deve ir no cabeçalho Authorization")
    void deveEnviarAuthorizationComChaveLegada() {
        SupabaseProperties legada = new SupabaseProperties(URL, "eyJlegada");
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer outro = MockRestServiceServer.bindTo(builder).build();
        SupabaseAuthClient clientLegado = new SupabaseAuthClient(SupabaseConfig.criarRestClient(builder, legada), legada);
        outro.expect(requestTo(URL + "/auth/v1/admin/users"))
                .andExpect(header("Authorization", "Bearer eyJlegada"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        clientLegado.criarUsuario("Maria", "maria@exemplo.com", "segredo1");

        outro.verify();
    }

    @Test
    @DisplayName("E-mail já cadastrado no Supabase vira EmailJaCadastradoException")
    void deveDetectarEmailExistente() {
        servidor.expect(requestTo(URL + "/auth/v1/admin/users"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\": 422, \"error_code\": \"email_exists\", \"msg\": \"already registered\"}"));

        assertThatThrownBy(() -> client.criarUsuario("Maria", "maria@exemplo.com", "segredo1"))
                .isInstanceOf(EmailJaCadastradoException.class)
                .hasMessageContaining("maria@exemplo.com");
    }

    @Test
    @DisplayName("Senha fraca recusada pelo Supabase vira 400")
    void deveRecusarSenhaFraca() {
        servidor.expect(requestTo(URL + "/auth/v1/admin/users"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error_code\": \"weak_password\", \"msg\": \"Password is too weak\"}"));

        assertThatThrownBy(() -> client.criarUsuario("Maria", "maria@exemplo.com", "123456"))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessageContaining("Password is too weak");
    }

    @Test
    @DisplayName("Chave secreta recusada (resposta sem JSON) é tratada como serviço indisponível")
    void deveTratarChaveRecusada() {
        servidor.expect(requestTo(URL + "/auth/v1/admin/users"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("invalid api key"));

        assertThatThrownBy(() -> client.criarUsuario("Maria", "maria@exemplo.com", "segredo1"))
                .isInstanceOf(ServicoDeUsuariosIndisponivelException.class);
    }

    @Test
    @DisplayName("Deve entrar com e-mail e senha e ler a sessão do Supabase")
    void deveEntrar() {
        servidor.expect(requestTo(URL + "/auth/v1/token?grant_type=password"))
                .andExpect(method(POST))
                .andExpect(content().json("{\"email\": \"maria@exemplo.com\", \"password\": \"segredo1\"}"))
                .andRespond(withSuccess(SESSAO, MediaType.APPLICATION_JSON));

        SessaoAuth sessao = client.entrar("maria@exemplo.com", "segredo1");

        assertThat(sessao.accessToken()).isEqualTo("token-acesso");
        assertThat(sessao.refreshToken()).isEqualTo("token-renovacao");
        assertThat(sessao.expiresAt()).isEqualTo(1793707200L);
        assertThat(sessao.user().id()).isEqualTo(ID);
        assertThat(sessao.user().appMetadata()).containsEntry("papel", "ADMIN");
        assertThat(sessao.user().userMetadata()).containsEntry("nome", "Maria");
    }

    @Test
    @DisplayName("Credenciais erradas viram CredenciaisInvalidasException")
    void deveRecusarCredenciais() {
        servidor.expect(requestTo(URL + "/auth/v1/token?grant_type=password"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error_code\": \"invalid_credentials\"}"));

        assertThatThrownBy(() -> client.entrar("maria@exemplo.com", "errada"))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha inválidos");
    }

    @Test
    @DisplayName("Deve renovar a sessão e recusar refresh token inválido")
    void deveRenovar() {
        servidor.expect(requestTo(URL + "/auth/v1/token?grant_type=refresh_token"))
                .andExpect(content().json("{\"refresh_token\": \"token-renovacao\"}"))
                .andRespond(withSuccess(SESSAO, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo(URL + "/auth/v1/token?grant_type=refresh_token"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThat(client.renovar("token-renovacao").accessToken()).isEqualTo("token-acesso");
        assertThatThrownBy(() -> client.renovar("usado"))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessageContaining("Sessão expirada");
    }

    @Test
    @DisplayName("Limite de requisições e erro 5xx viram serviço indisponível")
    void deveTratarFalhasDoSupabase() {
        servidor.expect(requestTo(URL + "/auth/v1/token?grant_type=password"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        servidor.expect(requestTo(URL + "/auth/v1/token?grant_type=password"))
                .andRespond(withServerError());
        servidor.expect(requestTo(URL + "/auth/v1/admin/users"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.entrar("a@b.com", "x")).isInstanceOf(ServicoDeUsuariosIndisponivelException.class);
        assertThatThrownBy(() -> client.entrar("a@b.com", "x")).isInstanceOf(ServicoDeUsuariosIndisponivelException.class);
        assertThatThrownBy(() -> client.criarUsuario("A", "a@b.com", "x"))
                .isInstanceOf(ServicoDeUsuariosIndisponivelException.class);
    }

    @Test
    @DisplayName("Deve alterar nome e e-mail pela API admin, mantendo o e-mail confirmado")
    void deveAtualizarDados() {
        servidor.expect(requestTo(URL + "/auth/v1/admin/users/" + ID))
                .andExpect(method(PUT))
                .andExpect(content().json("""
                        {"email": "nova@exemplo.com", "email_confirm": true, "user_metadata": {"nome": "Maria S."}}
                        """))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.atualizarDados(ID, "Maria S.", "nova@exemplo.com");

        servidor.verify();
    }

    @Test
    @DisplayName("Deve redefinir a senha enviando só o campo password")
    void deveRedefinirSenha() {
        servidor.expect(requestTo(URL + "/auth/v1/admin/users/" + ID))
                .andExpect(method(PUT))
                .andExpect(content().json("{\"password\": \"novaSenha1\"}", JsonCompareMode.STRICT))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.redefinirSenha(ID, "novaSenha1");

        servidor.verify();
    }

    @Test
    @DisplayName("Na edição: e-mail de outra conta vira 409, id inexistente vira 404 e senha fraca vira 400")
    void deveTraduzirErrosDaEdicao() {
        servidor.expect(requestTo(URL + "/auth/v1/admin/users/" + ID))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error_code\": \"email_exists\"}"));
        servidor.expect(requestTo(URL + "/auth/v1/admin/users/" + ID))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error_code\": \"user_not_found\"}"));
        servidor.expect(requestTo(URL + "/auth/v1/admin/users/" + ID))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error_code\": \"weak_password\", \"msg\": \"Password is too weak\"}"));
        servidor.expect(requestTo(URL + "/auth/v1/admin/users/" + ID))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.atualizarDados(ID, "Maria", "joao@exemplo.com"))
                .isInstanceOf(EmailJaCadastradoException.class)
                .hasMessageContaining("joao@exemplo.com");
        assertThatThrownBy(() -> client.redefinirSenha(ID, "novaSenha1"))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
        assertThatThrownBy(() -> client.redefinirSenha(ID, "123456"))
                .isInstanceOf(RequisicaoInvalidaException.class);
        assertThatThrownBy(() -> client.redefinirSenha(ID, "novaSenha1"))
                .isInstanceOf(ServicoDeUsuariosIndisponivelException.class);
    }

    @Test
    @DisplayName("Sem SUPABASE_SECRET_KEY não chama o Supabase e responde serviço indisponível")
    void deveExigirChave() {
        SupabaseProperties semChave = new SupabaseProperties(URL, "");
        SupabaseAuthClient semConfiguracao = new SupabaseAuthClient(
                SupabaseConfig.criarRestClient(RestClient.builder(), semChave), semChave);

        assertThatThrownBy(() -> semConfiguracao.entrar("a@b.com", "x"))
                .isInstanceOf(ServicoDeUsuariosIndisponivelException.class)
                .hasMessageContaining("SUPABASE_SECRET_KEY");
        assertThatThrownBy(() -> semConfiguracao.criarUsuario("A", "a@b.com", "x"))
                .isInstanceOf(ServicoDeUsuariosIndisponivelException.class);
    }
}
