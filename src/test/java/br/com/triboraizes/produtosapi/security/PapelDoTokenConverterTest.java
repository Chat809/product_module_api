package br.com.triboraizes.produtosapi.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PapelDoTokenConverterTest {

    private final PapelDoTokenConverter conversor = new PapelDoTokenConverter();

    private static Jwt token(Map<String, Object> claims) {
        Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "ES256").subject("u");
        claims.forEach(builder::claim);
        return builder.build();
    }

    @Test
    @DisplayName("app_metadata.papel = ADMIN vira ROLE_ADMIN")
    void deveConverterAdmin() {
        assertThat(conversor.convert(token(Map.of("app_metadata", Map.of("papel", "ADMIN")))))
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Papel ausente, desconhecido ou sem app_metadata vira ROLE_USUARIO")
    void deveCairParaUsuario() {
        assertThat(conversor.convert(token(Map.of("app_metadata", Map.of("papel", "USUARIO")))))
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USUARIO");
        assertThat(conversor.convert(token(Map.of("app_metadata", Map.of("papel", "dono")))))
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USUARIO");
        assertThat(conversor.convert(token(Map.of("email", "a@b.com"))))
                .extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_USUARIO");
    }
}
