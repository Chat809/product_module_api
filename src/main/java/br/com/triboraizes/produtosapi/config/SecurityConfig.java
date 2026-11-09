package br.com.triboraizes.produtosapi.config;

import br.com.triboraizes.produtosapi.model.Modulo;
import br.com.triboraizes.produtosapi.model.Submodulo;
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
 * e não guarda sessão. Cada rota exige a subaba correspondente ({@code SUB_*}); leituras usadas como opções em
 * outras telas exigem só o módulo ({@code MODULO_*}). "Meu perfil" e notificações valem para qualquer usuário logado,
 * e o webhook de entrada é público, autenticado pelo token da própria URL. Erros 401 e 403 são repassados ao {@code GlobalExceptionHandler} para sair em ProblemDetail.
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
                .headers(headers -> headers.frameOptions(frame -> frame.deny()))
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rotas -> rotas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/webhooks/entrada/*").permitAll()
                        .requestMatchers("/api/v1/usuarios/me", "/api/v1/usuarios/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/usuarios/opcoes")
                        .hasAnyAuthority(Modulo.DEMANDAS.autoridade(), Modulo.CRM.autoridade())
                        .requestMatchers("/api/v1/usuarios/**").hasAuthority(Submodulo.CONFIGURACOES_USUARIOS.autoridade())
                        .requestMatchers("/api/v1/demandas/**").hasAuthority(Submodulo.DEMANDAS_QUADRO.autoridade())
                        .requestMatchers("/api/v1/financeiro/resumo").hasAuthority(Modulo.FINANCEIRO.autoridade())
                        .requestMatchers("/api/v1/financeiro/contas-pagar", "/api/v1/financeiro/contas-pagar/**")
                        .hasAuthority(Submodulo.FINANCEIRO_CONTAS_PAGAR.autoridade())
                        .requestMatchers("/api/v1/financeiro/contas-receber", "/api/v1/financeiro/contas-receber/**")
                        .hasAuthority(Submodulo.FINANCEIRO_CONTAS_RECEBER.autoridade())
                        // Fornecedores e centros de custo aparecem como opções nos lançamentos de qualquer subaba do financeiro.
                        .requestMatchers(HttpMethod.GET, "/api/v1/financeiro/fornecedores", "/api/v1/financeiro/fornecedores/**",
                                "/api/v1/financeiro/centros-custo", "/api/v1/financeiro/centros-custo/**")
                        .hasAuthority(Modulo.FINANCEIRO.autoridade())
                        .requestMatchers("/api/v1/financeiro/fornecedores", "/api/v1/financeiro/fornecedores/**")
                        .hasAuthority(Submodulo.FINANCEIRO_FORNECEDORES.autoridade())
                        .requestMatchers("/api/v1/financeiro/centros-custo", "/api/v1/financeiro/centros-custo/**")
                        .hasAuthority(Submodulo.FINANCEIRO_CENTROS_CUSTO.autoridade())
                        .requestMatchers(HttpMethod.GET, "/api/v1/crm/clientes", "/api/v1/crm/clientes/**")
                        .hasAnyAuthority(Modulo.CRM.autoridade(), Modulo.FINANCEIRO.autoridade())
                        .requestMatchers("/api/v1/crm/clientes", "/api/v1/crm/clientes/**")
                        .hasAuthority(Submodulo.CRM_CLIENTES.autoridade())
                        .requestMatchers("/api/v1/crm/leads", "/api/v1/crm/leads/**")
                        .hasAuthority(Submodulo.CRM_LEADS.autoridade())
                        .requestMatchers("/api/v1/crm/reclamacoes", "/api/v1/crm/reclamacoes/**")
                        .hasAuthority(Submodulo.CRM_RECLAMACOES.autoridade())
                        .requestMatchers(HttpMethod.GET, "/api/v1/crm/parametros/**").hasAuthority(Modulo.CRM.autoridade())
                        .requestMatchers("/api/v1/crm/parametros/**")
                        .hasAuthority(Submodulo.CONFIGURACOES_PARAMETROS_CRM.autoridade())
                        .requestMatchers("/api/v1/relatorios/financeiro/**")
                        .hasAuthority(Submodulo.RELATORIOS_FINANCEIRO.autoridade())
                        .requestMatchers("/api/v1/relatorios/comercial/**")
                        .hasAuthority(Submodulo.RELATORIOS_COMERCIAL.autoridade())
                        .requestMatchers("/api/v1/relatorios/estoque/**")
                        .hasAuthority(Submodulo.RELATORIOS_ESTOQUE.autoridade())
                        .requestMatchers("/api/v1/relatorios/atendimento/**")
                        .hasAuthority(Submodulo.RELATORIOS_ATENDIMENTO.autoridade())
                        .requestMatchers("/api/v1/notificacoes", "/api/v1/notificacoes/**").authenticated()
                        .requestMatchers("/api/v1/configuracoes/webhooks/**")
                        .hasAuthority(Submodulo.CONFIGURACOES_WEBHOOKS.autoridade())
                        .requestMatchers("/api/v1/configuracoes/integracoes/**")
                        .hasAuthority(Submodulo.CONFIGURACOES_INTEGRACOES.autoridade())
                        .requestMatchers(HttpMethod.GET, "/api/v1/produtos/resumo")
                        .hasAuthority(Submodulo.INICIO_DASHBOARD.autoridade())
                        // O dashboard também lê o catálogo e o histórico; alterar qualquer coisa exige a subaba de produtos.
                        .requestMatchers(HttpMethod.GET, "/api/v1/produtos/**")
                        .hasAnyAuthority(Submodulo.INICIO_DASHBOARD.autoridade(), Submodulo.ESTOQUE_PRODUTOS.autoridade())
                        .requestMatchers("/api/v1/produtos/**").hasAuthority(Submodulo.ESTOQUE_PRODUTOS.autoridade())
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
