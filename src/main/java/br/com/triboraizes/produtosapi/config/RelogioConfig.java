package br.com.triboraizes.produtosapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** "Hoje" (prazos atrasados, contas vencidas) segue o fuso de Brasília, não o do servidor. */
@Configuration
public class RelogioConfig {

    public static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Bean
    public Clock relogio() {
        return Clock.system(FUSO);
    }
}
