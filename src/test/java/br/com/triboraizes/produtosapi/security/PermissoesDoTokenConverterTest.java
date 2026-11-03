package br.com.triboraizes.produtosapi.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PermissoesDoTokenConverterTest {

    private final PermissoesDoTokenConverter conversor = new PermissoesDoTokenConverter();

    private static Jwt token(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "ES256").subject("u");
        claims.forEach(builder::claim);
        return builder.build();
    }

    private List<String> autoridades(Map<String, Object> appMetadata) {
        return conversor.convert(token(Map.of("app_metadata", appMetadata))).stream()
                .map(GrantedAuthority::getAuthority).toList();
    }

    @Test
    @DisplayName("ADMIN recebe ROLE_ADMIN e todos os módulos, mesmo com acessos limitados no token")
    void deveLiberarTudoParaAdmin() {
        assertThat(autoridades(Map.of("papel", "ADMIN", "acessos", List.of("dashboard"))))
                .containsExactly("ROLE_ADMIN", "MODULO_DASHBOARD", "MODULO_PRODUTOS", "MODULO_USUARIOS");
    }

    @Test
    @DisplayName("USUARIO recebe só os módulos da lista")
    void deveLimitarUsuarioAosAcessos() {
        assertThat(autoridades(Map.of("papel", "USUARIO", "acessos", List.of("dashboard"))))
                .containsExactly("ROLE_USUARIO", "MODULO_DASHBOARD");
        assertThat(autoridades(Map.of("papel", "USUARIO", "acessos", List.of())))
                .containsExactly("ROLE_USUARIO");
    }

    @Test
    @DisplayName("'*' para USUARIO libera todos os módulos, exceto o de usuários")
    void deveTratarAsteriscoDoUsuario() {
        assertThat(autoridades(Map.of("papel", "USUARIO", "acessos", List.of("*", "usuarios"))))
                .containsExactly("ROLE_USUARIO", "MODULO_DASHBOARD", "MODULO_PRODUTOS");
    }

    @Test
    @DisplayName("Token sem acessos (anterior à coluna) ou sem app_metadata recebe o padrão")
    void deveCairParaPadrao() {
        assertThat(autoridades(Map.of("papel", "dono")))
                .containsExactly("ROLE_USUARIO", "MODULO_DASHBOARD", "MODULO_PRODUTOS");
        assertThat(conversor.convert(token(Map.of("email", "a@b.com")))).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USUARIO", "MODULO_DASHBOARD", "MODULO_PRODUTOS");
    }
}
