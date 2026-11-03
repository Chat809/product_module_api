package br.com.triboraizes.produtosapi.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(SupabaseProperties.class)
public class SupabaseConfig {

    @Bean
    public RestClient supabaseRestClient(SupabaseProperties properties) {
        return criarRestClient(RestClient.builder(), properties);
    }

    /**
     * Chaves novas ({@code sb_secret_...}) vão só no cabeçalho {@code apikey}; a chave legada
     * {@code service_role} é um JWT e também precisa ir em {@code Authorization}.
     */
    public static RestClient criarRestClient(RestClient.Builder builder, SupabaseProperties properties) {
        builder.baseUrl(properties.url());
        if (properties.configurado()) {
            builder.defaultHeader("apikey", properties.secretKey());
            if (!properties.secretKey().startsWith("sb_")) {
                builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.secretKey());
            }
        }
        return builder.build();
    }
}
