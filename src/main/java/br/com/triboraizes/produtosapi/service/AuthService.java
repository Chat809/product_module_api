package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.client.SupabaseAuthClient;
import br.com.triboraizes.produtosapi.client.SupabaseAuthClient.SessaoAuth;
import br.com.triboraizes.produtosapi.dto.GoogleLoginRequestDTO;
import br.com.triboraizes.produtosapi.dto.LoginRequestDTO;
import br.com.triboraizes.produtosapi.dto.RegistroRequestDTO;
import br.com.triboraizes.produtosapi.dto.SessaoDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioDTO;
import br.com.triboraizes.produtosapi.model.Acessos;
import br.com.triboraizes.produtosapi.model.Papel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/** Registro, login (e-mail e senha ou Google) e renovação de sessão. Quem guarda as senhas e emite os tokens é o Supabase Auth. */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SupabaseAuthClient authClient;

    public AuthService(SupabaseAuthClient authClient) {
        this.authClient = authClient;
    }

    /** Novas contas entram sempre como {@link Papel#USUARIO}; o papel é definido no banco, não pelo cliente. */
    public SessaoDTO registrar(RegistroRequestDTO request) {
        String email = normalizar(request.email());
        authClient.criarUsuario(request.nome().trim(), email, request.senha());
        log.info("Conta criada: {}", email);
        return paraSessao(authClient.entrar(email, request.senha()));
    }

    public SessaoDTO entrar(LoginRequestDTO request) {
        return paraSessao(authClient.entrar(normalizar(request.email()), request.senha()));
    }

    /**
     * Quem já tinha conta com o mesmo e-mail entra nela, com papel e acessos preservados: o Supabase
     * vincula o Google à conta existente. E-mails novos ganham conta de USUARIO pelo gatilho do banco.
     */
    public SessaoDTO entrarComGoogle(GoogleLoginRequestDTO request) {
        SessaoDTO sessao = paraSessao(authClient.trocarCodigo(request.codigo(), request.verificador()));
        log.info("Login com Google: {}", sessao.usuario().email());
        return sessao;
    }

    public SessaoDTO renovar(String refreshToken) {
        return paraSessao(authClient.renovar(refreshToken));
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** "nome" vem do registro por e-mail; "full_name" e "name" vêm do perfil do Google. */
    private static String nomeDe(Map<String, Object> dados, String email) {
        return Stream.of("nome", "full_name", "name")
                .map(dados::get)
                .filter(valor -> valor instanceof String texto && !texto.isBlank())
                .map(String::valueOf)
                .findFirst()
                .orElse(email);
    }

    private static SessaoDTO paraSessao(SessaoAuth sessao) {
        SupabaseAuthClient.UsuarioAuth user = sessao.user();
        Map<String, Object> app = user.appMetadata() == null ? Map.of() : user.appMetadata();
        Map<String, Object> dados = user.userMetadata() == null ? Map.of() : user.userMetadata();
        String nome = nomeDe(dados, user.email());
        Papel papel = Papel.de(app.get("papel"));
        UsuarioDTO usuario = new UsuarioDTO(user.id(), nome, user.email(), papel,
                Acessos.de(app.get("acessos"), papel), null, null);
        return new SessaoDTO(sessao.accessToken(), sessao.refreshToken(), Instant.ofEpochSecond(sessao.expiresAt()), usuario);
    }
}
