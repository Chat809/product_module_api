package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.Produto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Representação de um produto retornada pela API")
public record ProdutoResponseDTO(
        UUID id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer quantidadeEmEstoque,
        Instant criadoEm,
        Instant atualizadoEm,
        @Schema(description = "Versão do registro, usada para detectar edições concorrentes")
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
