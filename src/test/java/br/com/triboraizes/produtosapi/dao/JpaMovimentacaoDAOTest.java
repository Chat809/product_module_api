package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.MovimentacaoEstoque;
import br.com.triboraizes.produtosapi.model.Produto;
import br.com.triboraizes.produtosapi.model.TipoMovimentacao;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(DAOFactory.class)
class JpaMovimentacaoDAOTest {

    @Autowired
    private DAOFactory daoFactory;

    @Autowired
    private EntityManager entityManager;

    private MovimentacaoDAO movimentacaoDAO;

    private Produto teclado;

    private Produto mouse;

    @BeforeEach
    void setUp() {
        movimentacaoDAO = daoFactory.criarMovimentacaoDAO();
        ProdutoDAO produtoDAO = daoFactory.criarProdutoDAO();
        teclado = produtoDAO.salvar(new Produto("Teclado", null, new BigDecimal("349.90"), 10));
        mouse = produtoDAO.salvar(new Produto("Mouse", null, new BigDecimal("89.90"), 10));
    }

    @Test
    @DisplayName("Deve salvar uma movimentação gerando id e data")
    void deveSalvarMovimentacao() {
        MovimentacaoEstoque movimentacao = movimentacaoDAO.salvar(
                new MovimentacaoEstoque(teclado, TipoMovimentacao.ENTRADA, 10, 0, "Estoque inicial"));

        assertThat(movimentacao.getId()).isNotNull();
        assertThat(movimentacao.getCriadoEm()).isNotNull();
        assertThat(movimentacao.getSaldoApos()).isEqualTo(10);
    }

    @Test
    @DisplayName("Deve listar e contar somente o histórico do produto informado, paginado")
    void deveListarHistoricoDoProduto() {
        movimentacaoDAO.salvar(new MovimentacaoEstoque(teclado, TipoMovimentacao.ENTRADA, 10, 0, null));
        movimentacaoDAO.salvar(new MovimentacaoEstoque(teclado, TipoMovimentacao.ENTRADA, 10, 0, null));
        movimentacaoDAO.salvar(new MovimentacaoEstoque(mouse, TipoMovimentacao.ENTRADA, 10, 0, null));
        limparContexto();

        assertThat(movimentacaoDAO.listarPorProduto(teclado, 0, 1)).hasSize(1);
        assertThat(movimentacaoDAO.listarPorProduto(teclado, 0, 10))
                .allSatisfy(m -> assertThat(m.getProduto().getId()).isEqualTo(teclado.getId()));
        assertThat(movimentacaoDAO.contarPorProduto(teclado)).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve remover todo o histórico do produto informado")
    void deveRemoverHistoricoDoProduto() {
        movimentacaoDAO.salvar(new MovimentacaoEstoque(teclado, TipoMovimentacao.ENTRADA, 10, 0, null));
        movimentacaoDAO.salvar(new MovimentacaoEstoque(mouse, TipoMovimentacao.ENTRADA, 10, 0, null));

        movimentacaoDAO.removerPorProduto(teclado);
        limparContexto();

        assertThat(movimentacaoDAO.contarPorProduto(teclado)).isZero();
        assertThat(movimentacaoDAO.contarPorProduto(mouse)).isEqualTo(1);
    }

    private void limparContexto() {
        entityManager.flush();
        entityManager.clear();
    }
}
