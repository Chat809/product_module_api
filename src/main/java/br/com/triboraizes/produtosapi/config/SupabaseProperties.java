package br.com.triboraizes.produtosapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Endereço do projeto Supabase e chave secreta usada pelo backend.
 * A chave vem da variável de ambiente {@code SUPABASE_SECRET_KEY} e nunca deve ser versionada.
 */
@ConfigurationProperties("app.supabase")
public record SupabaseProperties(String url, String secretKey) {

    public boolean configurado() {
        return secretKey != null && !secretKey.isBlank();
    }

    public String emissorDosTokens() {
        return url + "/auth/v1";
    }

    public String jwksUri() {
        return url + "/auth/v1/.well-known/jwks.json";
    }
}
