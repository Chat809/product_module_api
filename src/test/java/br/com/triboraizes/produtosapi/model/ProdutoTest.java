package br.com.triboraizes.produtosapi.model;

import br.com.triboraizes.produtosapi.exception.EstoqueInsuficienteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Regras de negócio da entidade, sem Spring nem banco de dados. */
class ProdutoTest {

    private static Produto produtoComEstoque(int quantidade) {
        return new Produto("Teclado", "Mecânico", new BigDecimal("349.90"), quantidade);
    }

    @Test
    @DisplayName("Deve remover espaços das pontas do nome e da descrição e arredondar o preço para 2 casas")
    void deveNormalizarDadosNaCriacao() {
        Produto produto = new Produto("  Teclado  ", "  Mecânico ", new BigDecimal("10.005"), 3);

        assertThat(produto.getNome()).isEqualTo("Teclado");
        assertThat(produto.getDescricao()).isEqualTo("Mecânico");
        assertThat(produto.getPreco()).isEqualByComparingTo("10.01");
        assertThat(produto.getPreco().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve aceitar descrição nula")
    void deveAceitarDescricaoNula() {
        Produto produto = new Produto("Teclado", null, BigDecimal.TEN, 0);

        assertThat(produto.getDescricao()).isNull();
    }

    @Test
    @DisplayName("Atualizar deve substituir todos os dados")
    void deveAtualizarTodosOsDados() {
        Produto produto = produtoComEstoque(5);

        produto.atualizar("Mouse", "Sem fio", new BigDecimal("99.9"), 12);

        assertThat(produto.getNome()).isEqualTo("Mouse");
        assertThat(produto.getDescricao()).isEqualTo("Sem fio");
        assertThat(produto.getPreco()).isEqualByComparingTo("99.90");
        assertThat(produto.getQuantidadeEmEstoque()).isEqualTo(12);
    }

    @Test
    @DisplayName("Entrada deve somar ao estoque")
    void entradaDeveSomarAoEstoque() {
        Produto produto = produtoComEstoque(5);

        produto.registrarEntrada(7);

        assertThat(produto.getQuantidadeEmEstoque()).isEqualTo(12);
    }

    @Test
    @DisplayName("Saída deve subtrair do estoque e pode zerá-lo")
    void saidaDeveSubtrairDoEstoque() {
        Produto produto = produtoComEstoque(5);

        produto.registrarSaida(2);
        assertThat(produto.getQuantidadeEmEstoque()).isEqualTo(3);

        produto.registrarSaida(3);
        assertThat(produto.getQuantidadeEmEstoque()).isZero();
    }

    @Test
    @DisplayName("Saída maior que o estoque deve ser recusada sem alterar o saldo")
    void saidaMaiorQueEstoqueDeveSerRecusada() {
        Produto produto = produtoComEstoque(5);

        assertThatThrownBy(() -> produto.registrarSaida(6))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("há 5 unidade(s)")
                .hasMessageContaining("é de 6");
        assertThat(produto.getQuantidadeEmEstoque()).isEqualTo(5);
    }

    @ParameterizedTest(name = "quantidade {0}")
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("Entrada e saída devem exigir quantidade maior que zero")
    void movimentacaoDeveExigirQuantidadePositiva(int quantidade) {
        Produto produto = produtoComEstoque(5);

        assertThatThrownBy(() -> produto.registrarEntrada(quantidade))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> produto.registrarSaida(quantidade))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(produto.getQuantidadeEmEstoque()).isEqualTo(5);
    }
}
