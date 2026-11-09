package br.com.triboraizes.produtosapi.security;

import br.com.triboraizes.produtosapi.model.Acessos;
import br.com.triboraizes.produtosapi.model.Modulo;
import br.com.triboraizes.produtosapi.model.Papel;
import br.com.triboraizes.produtosapi.model.Submodulo;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converte {@code app_metadata.papel} e {@code app_metadata.acessos} do JWT do Supabase em autoridades:
 * {@code ROLE_ADMIN} ou {@code ROLE_USUARIO}, um {@code SUB_*} por subaba liberada e um {@code MODULO_*}
 * por módulo com ao menos uma subaba liberada.
 * O {@code app_metadata} só pode ser alterado com a chave secreta, não pelo próprio usuário.
 */
public class PermissoesDoTokenConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> appMetadata = jwt.getClaimAsMap("app_metadata");
        Map<String, Object> app = appMetadata == null ? Map.of() : appMetadata;
        Papel papel = Papel.de(app.get("papel"));
        List<GrantedAuthority> autoridades = new ArrayList<>();
        autoridades.add(new SimpleGrantedAuthority("ROLE_" + papel.name()));
        Set<Submodulo> subabas = Acessos.liberados(papel, Acessos.de(app.get("acessos"), papel));
        subabas.stream().map(Submodulo::autoridade).map(SimpleGrantedAuthority::new).forEach(autoridades::add);
        Acessos.modulos(subabas).stream()
                .map(Modulo::autoridade)
                .map(SimpleGrantedAuthority::new)
                .forEach(autoridades::add);
        return autoridades;
    }
}
