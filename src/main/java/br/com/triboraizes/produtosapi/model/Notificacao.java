package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Aviso para um usuário, gravado pelos gatilhos do banco e pela rotina diária de vencimentos. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Notificacao(
        UUID id,
        String tipo,
        String titulo,
        String mensagem,
        String link,
        @JsonAlias("lida_em") OffsetDateTime lidaEm,
        @JsonAlias("criado_em") OffsetDateTime criadoEm
) {
    public static final String COLUNAS = "id,tipo,titulo,mensagem,link,lida_em,criado_em";
}
