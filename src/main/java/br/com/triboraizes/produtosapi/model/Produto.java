package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

/**
 * Linha da tabela {@code public.produtos}. Versão, datas e o histórico de estoque
 * são mantidos por gatilhos do banco a cada gravação.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Produto(
        UUID id,
        String codigo,
        String nome,
        String descricao,
        String categoria,
        UnidadeMedida unidade,
        BigDecimal preco,
        BigDecimal custo,
        @JsonProperty("quantidade_em_estoque") Integer quantidadeEmEstoque,
        Boolean ativo,
        @JsonProperty("criado_por") UUID criadoPor,
        @JsonProperty("atualizado_por") UUID atualizadoPor,
        @JsonProperty("criado_em") OffsetDateTime criadoEm,
        @JsonProperty("atualizado_em") OffsetDateTime atualizadoEm,
        Long versao
) {

    /**
     * Dados digitados já no formato gravado: código em maiúsculas, textos sem espaços nas pontas
     * (vazios viram {@code null}), valores com 2 casas, unidade {@code UN} e ativo por padrão.
     */
    public static Produto novo(String codigo, String nome, String descricao, String categoria, UnidadeMedida unidade,
                               BigDecimal preco, BigDecimal custo, Integer quantidadeEmEstoque, Boolean ativo,
                               UUID usuario) {
        String codigoNormalizado = textoOuNulo(codigo);
        return new Produto(null,
                codigoNormalizado == null ? null : codigoNormalizado.toUpperCase(Locale.ROOT),
                nome == null ? null : nome.strip(),
                textoOuNulo(descricao),
                textoOuNulo(categoria),
                unidade == null ? UnidadeMedida.UN : unidade,
                duasCasas(preco),
                duasCasas(custo),
                quantidadeEmEstoque,
                ativo == null || ativo,
                usuario, usuario, null, null, null);
    }

    private static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private static BigDecimal duasCasas(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP);
    }
}
