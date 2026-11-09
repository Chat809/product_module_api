package br.com.triboraizes.produtosapi.dto;

import br.com.triboraizes.produtosapi.model.Produto;
import br.com.triboraizes.produtosapi.model.UnidadeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Dados para criação ou atualização de um produto")
public record ProdutoRequestDTO(

        @Schema(description = "Código interno (SKU), único. Gravado em maiúsculas; vazio fica sem código",
                example = "PER-001", nullable = true)
        @Pattern(regexp = "^\\s*([A-Za-z0-9][A-Za-z0-9._/-]{0,39})?\\s*$",
                message = "O código deve ter até 40 letras, números ou . _ / -, começando por letra ou número")
        String codigo,

        @Schema(example = "Teclado mecânico")
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres")
        String nome,

        @Schema(example = "Teclado mecânico ABNT2 com switches marrons", nullable = true)
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
        String descricao,

        @Schema(example = "Periféricos", nullable = true)
        @Size(max = 60, message = "A categoria deve ter no máximo 60 caracteres")
        String categoria,

        @Schema(description = "Unidade de venda e de contagem do estoque; omitida, vale UN", example = "UN",
                nullable = true)
        UnidadeMedida unidade,

        @Schema(description = "Preço de venda", example = "349.90")
        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero")
        @Digits(integer = 10, fraction = 2, message = "O preço deve ter no máximo 10 dígitos inteiros e 2 decimais")
        BigDecimal preco,

        @Schema(description = "Custo de compra ou produção, para calcular a margem", example = "210.00", nullable = true)
        @DecimalMin(value = "0.00", message = "O custo não pode ser negativo")
        @Digits(integer = 10, fraction = 2, message = "O custo deve ter no máximo 10 dígitos inteiros e 2 decimais")
        BigDecimal custo,

        @Schema(example = "25")
        @NotNull(message = "A quantidade em estoque é obrigatória")
        @PositiveOrZero(message = "A quantidade em estoque não pode ser negativa")
        Integer quantidadeEmEstoque,

        @Schema(description = "Produtos inativos saem dos indicadores do dashboard; omitido, vale true", example = "true",
                nullable = true)
        Boolean ativo,

        @Schema(description = "Opcional no PUT: versão lida pelo cliente. Se o produto tiver sido alterado depois disso, "
                + "a API responde 409 em vez de sobrescrever a alteração. O cabeçalho If-Match, se enviado, "
                + "tem prioridade. Ignorado no POST", example = "0", nullable = true)
        Long versao
) {

    public Produto toProduto(UUID usuario) {
        return Produto.novo(codigo, nome, descricao, categoria, unidade, preco, custo, quantidadeEmEstoque, ativo, usuario);
    }

    public ProdutoRequestDTO comVersao(Long novaVersao) {
        return new ProdutoRequestDTO(codigo, nome, descricao, categoria, unidade, preco, custo, quantidadeEmEstoque, ativo,
                novaVersao);
    }
}
