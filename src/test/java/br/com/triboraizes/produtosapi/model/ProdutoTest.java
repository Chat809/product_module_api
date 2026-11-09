package br.com.triboraizes.produtosapi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Normalização dos dados digitados antes de irem para o banco, sem Spring nem rede. */
class ProdutoTest {

    private static final UUID USUARIO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Test
    @DisplayName("Deve remover espaços das pontas, pôr o código em maiúsculas e arredondar valores para 2 casas")
    void deveNormalizarDados() {
        Produto produto = Produto.novo("  per-001 ", "  Teclado  ", "  Mecânico ", " Periféricos ", UnidadeMedida.CX,
                new BigDecimal("10.005"), new BigDecimal("4.5"), 3, false, USUARIO);

        assertThat(produto.codigo()).isEqualTo("PER-001");
        assertThat(produto.nome()).isEqualTo("Teclado");
        assertThat(produto.descricao()).isEqualTo("Mecânico");
        assertThat(produto.categoria()).isEqualTo("Periféricos");
        assertThat(produto.unidade()).isEqualTo(UnidadeMedida.CX);
        assertThat(produto.preco()).isEqualByComparingTo("10.01");
        assertThat(produto.preco().scale()).isEqualTo(2);
        assertThat(produto.custo()).isEqualByComparingTo("4.50");
        assertThat(produto.ativo()).isFalse();
        assertThat(produto.criadoPor()).isEqualTo(USUARIO);
        assertThat(produto.atualizadoPor()).isEqualTo(USUARIO);
        assertThat(produto.id()).isNull();
        assertThat(produto.versao()).isNull();
    }

    @Test
    @DisplayName("Textos opcionais vazios viram nulos; unidade UN e ativo são o padrão")
    void deveAplicarPadroes() {
        Produto produto = Produto.novo(" ", "Mouse", "", "  ", null, new BigDecimal("89.9"), null, 0, null, null);

        assertThat(produto.codigo()).isNull();
        assertThat(produto.descricao()).isNull();
        assertThat(produto.categoria()).isNull();
        assertThat(produto.custo()).isNull();
        assertThat(produto.unidade()).isEqualTo(UnidadeMedida.UN);
        assertThat(produto.ativo()).isTrue();
    }

    @Test
    @DisplayName("Deve aceitar nome e preço nulos, deixando a validação para o DTO")
    void deveAceitarNulos() {
        Produto produto = Produto.novo(null, null, null, null, null, null, null, null, true, null);

        assertThat(produto.nome()).isNull();
        assertThat(produto.preco()).isNull();
    }
}
