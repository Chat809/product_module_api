package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.client.SupabaseAuthClient;
import br.com.triboraizes.produtosapi.client.SupabaseAuthClient.SessaoAuth;
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

/** Registro, login e renovação de sessão. Quem guarda as senhas e emite os tokens é o Supabase Auth. */
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

    public SessaoDTO renovar(String refreshToken) {
        return paraSessao(authClient.renovar(refreshToken));
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static SessaoDTO paraSessao(SessaoAuth sessao) {
        SupabaseAuthClient.UsuarioAuth user = sessao.user();
        Map<String, Object> app = user.appMetadata() == null ? Map.of() : user.appMetadata();
        Map<String, Object> dados = user.userMetadata() == null ? Map.of() : user.userMetadata();
        Object nome = dados.getOrDefault("nome", user.email());
        Papel papel = Papel.de(app.get("papel"));
        UsuarioDTO usuario = new UsuarioDTO(user.id(), String.valueOf(nome), user.email(), papel,
                Acessos.de(app.get("acessos"), papel), null);
        return new SessaoDTO(sessao.accessToken(), sessao.refreshToken(), Instant.ofEpochSecond(sessao.expiresAt()), usuario);
    }
}
