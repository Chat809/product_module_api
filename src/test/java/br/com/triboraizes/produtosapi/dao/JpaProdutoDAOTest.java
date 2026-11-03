package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.Produto;
import br.com.triboraizes.produtosapi.model.ResumoEstoque;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    @DisplayName("Deve salvar um produto gerando id UUID, datas de auditoria e versão")
    void deveSalvarProduto() {
        Produto produto = produtoDAO.salvar(novoProduto("Teclado"));

        assertThat(produto.getId()).isNotNull();
        assertThat(produto.getCriadoEm()).isNotNull();
        assertThat(produto.getAtualizadoEm()).isNotNull();
        assertThat(produto.getVersao()).isZero();
    }

    @Test
    @DisplayName("Deve buscar um produto salvo pelo id")
    void deveBuscarPorId() {
        UUID id = produtoDAO.salvar(novoProduto("Teclado")).getId();
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
        assertThat(produtoDAO.buscarPorId(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("Deve listar uma página de produtos na ordem pedida")
    void deveListarPaginaOrdenada() {
        produtoDAO.salvar(new Produto("Teclado", null, new BigDecimal("349.90"), 10));
        produtoDAO.salvar(new Produto("Mouse", null, new BigDecimal("89.90"), 10));
        produtoDAO.salvar(new Produto("Monitor", null, new BigDecimal("999.00"), 10));
        limparContexto();

        assertThat(produtoDAO.listar(null, 0, 2, "preco", false))
                .extracting(Produto::getNome)
                .containsExactly("Monitor", "Teclado");
        assertThat(produtoDAO.listar(null, 1, 2, "preco", false))
                .extracting(Produto::getNome)
                .containsExactly("Mouse");
        assertThat(produtoDAO.contar(null)).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve buscar pelo nome ou pela descrição sem diferenciar maiúsculas")
    void deveBuscarPorNomeOuDescricao() {
        produtoDAO.salvar(new Produto("Teclado mecânico", null, new BigDecimal("349.90"), 10));
        produtoDAO.salvar(new Produto("Mouse", "Acompanha TECLADO numérico", new BigDecimal("89.90"), 10));
        produtoDAO.salvar(new Produto("Monitor", "Tela 27 polegadas", new BigDecimal("999.00"), 10));
        limparContexto();

        assertThat(produtoDAO.listar("teclado", 0, 10, "nome", true))
                .extracting(Produto::getNome)
                .containsExactly("Mouse", "Teclado mecânico");
        assertThat(produtoDAO.contar("teclado")).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve calcular os indicadores do estoque")
    void deveResumirEstoque() {
        produtoDAO.salvar(new Produto("Teclado", null, new BigDecimal("100.00"), 10));
        produtoDAO.salvar(new Produto("Mouse", null, new BigDecimal("50.50"), 2));
        produtoDAO.salvar(new Produto("Monitor", null, new BigDecimal("999.00"), 0));
        limparContexto();

        assertThat(produtoDAO.resumir(5))
                .isEqualTo(new ResumoEstoque(3, 12, new BigDecimal("1101.00"), 1, 1));
    }

    @Test
    @DisplayName("Deve retornar indicadores zerados quando não houver produtos")
    void deveResumirEstoqueVazio() {
        assertThat(produtoDAO.resumir(5))
                .isEqualTo(new ResumoEstoque(0, 0, new BigDecimal("0.00"), 0, 0));
    }

    @Test
    @DisplayName("Deve recusar coluna de ordenação fora da lista permitida")
    void deveRecusarColunaDeOrdenacaoNaoPermitida() {
        assertThatThrownBy(() -> produtoDAO.listar(null, 0, 10, "id; drop table produtos", true))
                .isInstanceOf(IllegalArgumentException.class);
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
        assertThat(atualizado.getVersao()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve remover um produto")
    void deveRemoverProduto() {
        UUID id = produtoDAO.salvar(novoProduto("Teclado")).getId();
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
