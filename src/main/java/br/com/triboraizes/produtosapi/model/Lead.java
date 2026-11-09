package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Linha de {@code leads}. {@code ordem} posiciona o card dentro da etapa no funil; {@code clienteId} e
 * {@code convertidoEm} são preenchidos quando o lead vira cliente.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Lead(
        UUID id,
        String nome,
        String empresa,
        String email,
        String telefone,
        @JsonAlias("origem_id") UUID origemId,
        @JsonAlias("etapa_id") UUID etapaId,
        @JsonAlias("valor_estimado") BigDecimal valorEstimado,
        @JsonAlias("responsavel_id") UUID responsavelId,
        @JsonAlias("proxima_acao") String proximaAcao,
        @JsonAlias("proxima_acao_em") LocalDate proximaAcaoEm,
        @JsonAlias("motivo_perda") String motivoPerda,
        String observacoes,
        double ordem,
        @JsonAlias("cliente_id") UUID clienteId,
        @JsonAlias("convertido_em") OffsetDateTime convertidoEm,
        @JsonAlias("criado_em") OffsetDateTime criadoEm,
        @JsonAlias("atualizado_em") OffsetDateTime atualizadoEm
) {
    public static final String COLUNAS = "*";
}
