package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.Produto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Representação de um produto retornada pela API")
public record ProdutoResponseDTO(
        @Schema(example = "3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10")
        UUID id,
        @Schema(example = "Teclado mecânico")
        String nome,
        @Schema(example = "Teclado mecânico ABNT2 com switches marrons", nullable = true)
        String descricao,
        @Schema(example = "349.90")
        BigDecimal preco,
        @Schema(example = "25")
        Integer quantidadeEmEstoque,
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
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getPreco(),
                produto.getQuantidadeEmEstoque(),
                produto.getCriadoEm(),
                produto.getAtualizadoEm(),
                produto.getVersao()
        );
    }
}
