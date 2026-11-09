package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Se o usuário quer receber um tipo de notificação no painel e por e-mail. Sem linha gravada, vale o padrão. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PreferenciaNotificacao(
        String tipo,
        @JsonAlias("no_app") boolean noApp,
        @JsonAlias("por_email") boolean porEmail
) {
    public static final String COLUNAS = "tipo,no_app,por_email";
}
