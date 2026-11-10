package br.com.triboraizes.produtosapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Liga o {@code @Async}, usado para entregar webhooks fora da requisição que gerou o evento. */
@Configuration
@EnableAsync
public class AsyncConfig {
}
