package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.ProdutoDAO;
import br.com.triboraizes.produtosapi.dto.ProdutoRequestDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoResponseDTO;
import br.com.triboraizes.produtosapi.exception.ProdutoNaoEncontradoException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private DAOFactory daoFactory;

    @Mock
    private ProdutoDAO produtoDAO;

    private ProdutoService produtoService;

    @BeforeEach
    void setUp() {
        when(daoFactory.criarProdutoDAO()).thenReturn(produtoDAO);
        produtoService = new ProdutoService(daoFactory);
    }

    @Test
    @DisplayName("Deve criar um produto e retornar seus dados com o id gerado")
    void deveCriarProduto() {
        // given
        ProdutoRequestDTO request = new ProdutoRequestDTO("  Teclado  ", "Mecânico", new BigDecimal("349.90"), 25);
        when(produtoDAO.salvar(any(Produto.class))).thenAnswer(invocation -> {
            Produto produto = invocation.getArgument(0);
            ReflectionTestUtils.setField(produto, "id", 1L);
            return produto;
        });

        // when
        ProdutoResponseDTO response = produtoService.criar(request);

        // then
        assertThat(response).isEqualTo(new ProdutoResponseDTO(1L, "Teclado", "Mecânico", new BigDecimal("349.90"), 25));
        ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
        verify(produtoDAO).salvar(captor.capture());
        assertThat(captor.getValue().getNome()).isEqualTo("Teclado");
    }

    @Test
    @DisplayName("Deve atualizar todos os dados de um produto existente, normalizando o preço para 2 casas")
    void deveAtualizarProdutoExistente() {
        // given
        Produto existente = produtoComId(1L, "Teclado", "Antigo", "100.00", 5);
        when(produtoDAO.buscarPorId(1L)).thenReturn(Optional.of(existente));
        when(produtoDAO.atualizar(existente)).thenReturn(existente);
        ProdutoRequestDTO request = new ProdutoRequestDTO("Mouse", "Sem fio", new BigDecimal("89.9"), 10);

        // when
        ProdutoResponseDTO response = produtoService.atualizar(1L, request);

        // then
        assertThat(response).isEqualTo(new ProdutoResponseDTO(1L, "Mouse", "Sem fio", new BigDecimal("89.90"), 10));
        verify(produtoDAO).atualizar(existente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar um produto inexistente")
    void deveLancarExcecaoAoAtualizarProdutoInexistente() {
        // given
        when(produtoDAO.buscarPorId(99L)).thenReturn(Optional.empty());
        ProdutoRequestDTO request = new ProdutoRequestDTO("Mouse", null, new BigDecimal("89.90"), 10);

        // when / then
        assertThatThrownBy(() -> produtoService.atualizar(99L, request))
                .isInstanceOf(ProdutoNaoEncontradoException.class)
                .hasMessageContaining("99");
        verify(produtoDAO, never()).atualizar(any());
    }

    @Test
    @DisplayName("Deve remover um produto existente")
    void deveRemoverProdutoExistente() {
        // given
        Produto existente = produtoComId(1L, "Teclado", null, "100.00", 5);
        when(produtoDAO.buscarPorId(1L)).thenReturn(Optional.of(existente));

        // when
        produtoService.remover(1L);

        // then
        verify(produtoDAO).remover(existente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao remover um produto inexistente")
    void deveLancarExcecaoAoRemoverProdutoInexistente() {
        // given
        when(produtoDAO.buscarPorId(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> produtoService.remover(99L))
                .isInstanceOf(ProdutoNaoEncontradoException.class);
        verify(produtoDAO, never()).remover(any());
    }

    @Test
    @DisplayName("Deve retornar o produto quando o id existir")
    void deveBuscarProdutoPorId() {
        // given
        when(produtoDAO.buscarPorId(1L)).thenReturn(Optional.of(produtoComId(1L, "Teclado", "Mecânico", "349.90", 25)));

        // when
        ProdutoResponseDTO response = produtoService.buscarPorId(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nome()).isEqualTo("Teclado");
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar um produto inexistente")
    void deveLancarExcecaoAoBuscarProdutoInexistente() {
        // given
        when(produtoDAO.buscarPorId(99L)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> produtoService.buscarPorId(99L))
                .isInstanceOf(ProdutoNaoEncontradoException.class)
                .hasMessage("Produto com id 99 não encontrado");
    }

    @Test
    @DisplayName("Deve listar todos os produtos cadastrados")
    void deveListarTodosOsProdutos() {
        // given
        when(produtoDAO.listarTodos()).thenReturn(List.of(
                produtoComId(1L, "Teclado", null, "349.90", 25),
                produtoComId(2L, "Mouse", null, "89.90", 10)));

        // when
        List<ProdutoResponseDTO> response = produtoService.listarTodos();

        // then
        assertThat(response).extracting(ProdutoResponseDTO::id).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não houver produtos")
    void deveRetornarListaVaziaQuandoNaoHouverProdutos() {
        // given
        when(produtoDAO.listarTodos()).thenReturn(List.of());

        // when / then
        assertThat(produtoService.listarTodos()).isEmpty();
    }

    private static Produto produtoComId(Long id, String nome, String descricao, String preco, int quantidade) {
        Produto produto = new Produto(nome, descricao, new BigDecimal(preco), quantidade);
        ReflectionTestUtils.setField(produto, "id", id);
        return produto;
    }
}
