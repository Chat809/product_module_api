package br.com.triboraizes.produtosapi.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Produtos API",
        version = "v1",
        description = "API REST para gerenciamento de produtos"
))
public class OpenApiConfig {
}
