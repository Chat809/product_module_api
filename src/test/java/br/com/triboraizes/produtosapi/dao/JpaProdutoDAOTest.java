package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.Produto;
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
class JpaProdutoDAOTest {

    @Autowired
    private DAOFactory daoFactory;

    @Autowired
    private EntityManager entityManager;

    private ProdutoDAO produtoDAO;

    @BeforeEach
    void setUp() {
        produtoDAO = daoFactory.criarProdutoDAO();
    }

    @Test
    @DisplayName("Deve salvar um produto gerando o id")
    void deveSalvarProduto() {
        Produto produto = produtoDAO.salvar(novoProduto("Teclado"));

        assertThat(produto.getId()).isNotNull();
    }

    @Test
    @DisplayName("Deve buscar um produto salvo pelo id")
    void deveBuscarPorId() {
        Long id = produtoDAO.salvar(novoProduto("Teclado")).getId();
        limparContexto();

        assertThat(produtoDAO.buscarPorId(id))
                .hasValueSatisfying(produto -> {
                    assertThat(produto.getNome()).isEqualTo("Teclado");
                    assertThat(produto.getPreco()).isEqualByComparingTo("349.90");
                });
    }

    @Test
    @DisplayName("Deve retornar vazio ao buscar um id inexistente")
    void deveRetornarVazioQuandoIdNaoExistir() {
        assertThat(produtoDAO.buscarPorId(999L)).isEmpty();
    }

    @Test
    @DisplayName("Deve listar todos os produtos ordenados pelo id")
    void deveListarTodos() {
        produtoDAO.salvar(novoProduto("Teclado"));
        produtoDAO.salvar(novoProduto("Mouse"));
        limparContexto();

        assertThat(produtoDAO.listarTodos())
                .extracting(Produto::getNome)
                .containsExactly("Teclado", "Mouse");
    }

    @Test
    @DisplayName("Deve persistir as alterações de um produto")
    void deveAtualizarProduto() {
        Produto produto = produtoDAO.salvar(novoProduto("Teclado"));
        produto.atualizar("Mouse", "Sem fio", new BigDecimal("89.90"), 3);

        produtoDAO.atualizar(produto);
        limparContexto();

        Produto atualizado = produtoDAO.buscarPorId(produto.getId()).orElseThrow();
        assertThat(atualizado.getNome()).isEqualTo("Mouse");
        assertThat(atualizado.getQuantidadeEmEstoque()).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve remover um produto")
    void deveRemoverProduto() {
        Long id = produtoDAO.salvar(novoProduto("Teclado")).getId();
        limparContexto();

        produtoDAO.remover(produtoDAO.buscarPorId(id).orElseThrow());
        limparContexto();

        assertThat(produtoDAO.buscarPorId(id)).isEmpty();
    }

    private void limparContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    private static Produto novoProduto(String nome) {
        return new Produto(nome, "Descrição", new BigDecimal("349.90"), 10);
    }
}
