package br.com.triboraizes.produtosapi.config;

import br.com.triboraizes.produtosapi.model.Modulo;
import br.com.triboraizes.produtosapi.security.PermissoesDoTokenConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.Collection;

/**
 * A API é um resource server: valida o JWT emitido pelo Supabase Auth (ES256, chaves públicas do JWKS)
 * e não guarda sessão. Produtos, demandas e financeiro exigem o módulo correspondente ({@code MODULO_*}),
 * a gestão de usuários exige o papel ADMIN e "Meu perfil" vale para qualquer usuário logado. Erros 401 e 403 são repassados ao {@code GlobalExceptionHandler} para sair em ProblemDetail.
 */
@Configuration
@EnableConfigurationProperties(SupabaseProperties.class)
public class SecurityConfig {

    private static final String AUDIENCIA = "authenticated";

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {
        AuthenticationEntryPoint naoAutenticado = (request, response, ex) ->
                resolver.resolveException(request, response, null, ex);
        AccessDeniedHandler acessoNegado = (request, response, ex) ->
                resolver.resolveException(request, response, null, ex);

        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(new PermissoesDoTokenConverter());

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/h2-console/**", "/error").permitAll()
                        .requestMatchers("/api/v1/usuarios/me", "/api/v1/usuarios/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/opcoes")
                        .hasAuthority(Modulo.DEMANDAS.autoridade())
                        .requestMatchers("/api/v1/usuarios/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/demandas/**").hasAuthority(Modulo.DEMANDAS.autoridade())
                        .requestMatchers("/api/v1/financeiro/**").hasAuthority(Modulo.FINANCEIRO.autoridade())
                        .requestMatchers(HttpMethod.GET, "/api/v1/produtos/resumo")
                        .hasAuthority(Modulo.DASHBOARD.autoridade())
                        // O dashboard também lê o catálogo e o histórico; alterar qualquer coisa exige o módulo de produtos.
                        .requestMatchers(HttpMethod.GET, "/api/v1/produtos/**")
                        .hasAnyAuthority(Modulo.DASHBOARD.autoridade(), Modulo.PRODUTOS.autoridade())
                        .requestMatchers("/api/v1/produtos/**").hasAuthority(Modulo.PRODUTOS.autoridade())
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversor))
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(acessoNegado))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(acessoNegado));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SupabaseProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwksUri())
                .jwsAlgorithm(SignatureAlgorithm.ES256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(properties.emissorDosTokens()),
                new JwtClaimValidator<>(JwtClaimNames.AUD, SecurityConfig::audienciaValida)));
        return decoder;
    }

    private static boolean audienciaValida(Object aud) {
        return aud instanceof Collection<?> valores ? valores.contains(AUDIENCIA) : AUDIENCIA.equals(aud);
    }
}
