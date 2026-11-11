package br.com.triboraizes.produtosapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Download público: liberado para qualquer origem, pois a página de download pode ser hospedada em qualquer lugar.
        // Precisa vir antes do mapeamento geral para ter prioridade no casamento de rota.
        registry.addMapping("/api/v1/download")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "OPTIONS")
                .exposedHeaders("Content-Disposition");

        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .exposedHeaders("Location", "ETag");
    }
}
