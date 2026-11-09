package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Linha imutável de {@code public.movimentacoes_estoque}: uma alteração no estoque de um {@link Produto}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MovimentacaoEstoque(
        UUID id,
        @JsonProperty("produto_id") UUID produtoId,
        TipoMovimentacao tipo,
        Integer quantidade,
        @JsonProperty("saldo_anterior") Integer saldoAnterior,
        @JsonProperty("saldo_apos") Integer saldoApos,
        String motivo,
        @JsonProperty("criado_por") UUID criadoPor,
        @JsonProperty("criado_em") OffsetDateTime criadoEm
) {
}
