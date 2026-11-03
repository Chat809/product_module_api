package br.com.triboraizes.produtosapi.security;

import br.com.triboraizes.produtosapi.model.Papel;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Lê o papel em {@code app_metadata.papel} do JWT do Supabase e o converte em {@code ROLE_ADMIN} ou
 * {@code ROLE_USUARIO}. O {@code app_metadata} só pode ser alterado com a chave secreta, não pelo próprio usuário.
 */
public class PapelDoTokenConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> appMetadata = jwt.getClaimAsMap("app_metadata");
        Papel papel = Papel.de(appMetadata == null ? null : appMetadata.get("papel"));
        return List.of(new SimpleGrantedAuthority("ROLE_" + papel.name()));
    }
}
