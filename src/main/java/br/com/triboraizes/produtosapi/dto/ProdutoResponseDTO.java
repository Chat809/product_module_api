package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.Produto;
import br.com.triboraizes.produtosapi.model.UnidadeMedida;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Representação de um produto retornada pela API")
public record ProdutoResponseDTO(
        @Schema(example = "3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10")
        UUID id,
        @Schema(example = "PER-001", nullable = true)
        String codigo,
        @Schema(example = "Teclado mecânico")
        String nome,
        @Schema(example = "Teclado mecânico ABNT2 com switches marrons", nullable = true)
        String descricao,
        @Schema(example = "Periféricos", nullable = true)
        String categoria,
        @Schema(example = "UN")
        UnidadeMedida unidade,
        @Schema(example = "349.90")
        BigDecimal preco,
        @Schema(example = "210.00", nullable = true)
        BigDecimal custo,
        @Schema(example = "25")
        Integer quantidadeEmEstoque,
        @Schema(example = "true")
        boolean ativo,
        @Schema(description = "Data de criação (UTC)", example = "2026-11-03T05:41:52.675Z")
        Instant criadoEm,
        @Schema(description = "Data da última alteração (UTC)", example = "2026-11-03T05:41:52.675Z")
        Instant atualizadoEm,
        @Schema(description = "Versão do registro, usada para detectar edições concorrentes. "
                + "É o mesmo valor do cabeçalho ETag", example = "0")
        Long versao
) {

    public static ProdutoResponseDTO from(Produto produto) {
        return new ProdutoResponseDTO(
                produto.id(),
                produto.codigo(),
                produto.nome(),
                produto.descricao(),
                produto.categoria(),
                produto.unidade(),
                produto.preco(),
                produto.custo(),
                produto.quantidadeEmEstoque(),
                !Boolean.FALSE.equals(produto.ativo()),
                instante(produto.criadoEm()),
                instante(produto.atualizadoEm()),
                produto.versao()
        );
    }

    private static Instant instante(OffsetDateTime data) {
        return data == null ? null : data.toInstant();
    }
}
