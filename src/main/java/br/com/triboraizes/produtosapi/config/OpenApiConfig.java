package br.com.triboraizes.produtosapi.config;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Produtos API",
                version = "v1",
                description = """
                        API REST para gerenciamento de produtos e do estoque.

                        **Links**
                        - [Painel administrativo (Angular)](https://productmoduleinterface.vercel.app), que consome esta API
                        - [Repositório da API](https://github.com/Chat809/product_module_api)
                        - [Repositório do painel](https://github.com/Chat809/product_module_interface)

                        **Como usar este Swagger:** escolha o servidor acima, abra um endpoint, clique em \
                        *Try it out* e depois em *Execute*. A API sobe com 15 produtos de exemplo; use \
                        `GET /api/v1/produtos` para obter um `id`.

                        **Convenções**
                        - Erros no formato [RFC 9457 (Problem Details)](https://www.rfc-editor.org/rfc/rfc9457), \
                        com `title`, `status`, `detail` e, em erros de validação, a lista `erros` por campo.
                        - Edição concorrente: `GET /{id}` devolve o cabeçalho `ETag`; envie-o em `If-Match` no \
                        `PUT`. Se o produto mudou nesse meio-tempo, a resposta é 409.
                        - No plano gratuito do Render, a primeira requisição depois de um tempo parado pode \
                        levar até 1 minuto.""",
                contact = @Contact(name = "Repositório no GitHub", url = "https://github.com/Chat809/product_module_api")
        ),
        servers = {
                @Server(url = "/", description = "Servidor desta página"),
                @Server(url = "https://product-module-api.onrender.com", description = "Produção (Render)")
        },
        externalDocs = @ExternalDocumentation(
                description = "README com arquitetura, decisões e exemplos",
                url = "https://github.com/Chat809/product_module_api#readme")
)
public class OpenApiConfig {
}
