package br.com.triboraizes.produtosapi.client;

import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.CredenciaisInvalidasException;
import br.com.triboraizes.produtosapi.exception.EmailJaCadastradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.exception.ServicoDeUsuariosIndisponivelException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.UUID;

/** Chamadas à API de autenticação do Supabase (GoTrue), sempre feitas pelo backend com a chave secreta. */
@Component
public class SupabaseAuthClient {

    private static final Logger log = LoggerFactory.getLogger(SupabaseAuthClient.class);

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UsuarioAuth(
            UUID id,
            String email,
            @JsonProperty("app_metadata") Map<String, Object> appMetadata,
            @JsonProperty("user_metadata") Map<String, Object> userMetadata
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SessaoAuth(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_at") long expiresAt,
            UsuarioAuth user
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ErroAuth(@JsonProperty("error_code") String codigo, String msg) {
    }

    private final RestClient restClient;
    private final SupabaseProperties properties;

    public SupabaseAuthClient(RestClient supabaseRestClient, SupabaseProperties properties) {
        this.restClient = supabaseRestClient;
        this.properties = properties;
    }

    /** Cria o usuário já com o e-mail confirmado, para que possa entrar logo em seguida. */
    public void criarUsuario(String nome, String email, String senha) {
        exigirConfiguracao();
        try {
            restClient.post()
                    .uri("/auth/v1/admin/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "email", email,
                            "password", senha,
                            "email_confirm", true,
                            "user_metadata", Map.of("nome", nome)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException ex) {
            ErroAuth erro = lerErro(ex);
            if ("email_exists".equals(erro.codigo()) || "user_already_exists".equals(erro.codigo())) {
                throw new EmailJaCadastradoException(email);
            }
            if ("weak_password".equals(erro.codigo()) || "validation_failed".equals(erro.codigo())) {
                throw new RequisicaoInvalidaException("Dados recusados pelo serviço de usuários: " + erro.msg());
            }
            throw indisponivel(ex);
        } catch (RestClientException ex) {
            throw indisponivel(ex);
        }
    }

    public SessaoAuth entrar(String email, String senha) {
        return pedirToken("password", Map.of("email", email, "password", senha), "E-mail ou senha inválidos");
    }

    public SessaoAuth renovar(String refreshToken) {
        return pedirToken("refresh_token", Map.of("refresh_token", refreshToken),
                "Sessão expirada. Faça login novamente.");
    }

    private SessaoAuth pedirToken(String grantType, Map<String, String> corpo, String mensagemDeRecusa) {
        exigirConfiguracao();
        try {
            return restClient.post()
                    .uri("/auth/v1/token?grant_type={grantType}", grantType)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(SessaoAuth.class);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 401) {
                throw new CredenciaisInvalidasException(mensagemDeRecusa);
            }
            throw indisponivel(ex);
        } catch (RestClientException ex) {
            throw indisponivel(ex);
        }
    }

    private void exigirConfiguracao() {
        if (!properties.configurado()) {
            throw new ServicoDeUsuariosIndisponivelException(
                    "Serviço de usuários não configurado (defina SUPABASE_SECRET_KEY)", null);
        }
    }

    private static ErroAuth lerErro(HttpClientErrorException ex) {
        try {
            ErroAuth erro = ex.getResponseBodyAs(ErroAuth.class);
            return erro != null ? erro : new ErroAuth(null, ex.getStatusText());
        } catch (RuntimeException naoEraJson) {
            return new ErroAuth(null, ex.getStatusText());
        }
    }

    private static ServicoDeUsuariosIndisponivelException indisponivel(RestClientException ex) {
        log.error("Falha ao chamar o Supabase Auth", ex);
        return new ServicoDeUsuariosIndisponivelException("Serviço de usuários indisponível. Tente novamente.", ex);
    }
}
