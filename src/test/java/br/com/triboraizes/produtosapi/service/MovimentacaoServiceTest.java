package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.MovimentacaoDAO;
import br.com.triboraizes.produtosapi.dao.ProdutoDAO;
import br.com.triboraizes.produtosapi.dto.MovimentacaoRequestDTO;
import br.com.triboraizes.produtosapi.dto.MovimentacaoResponseDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.exception.EstoqueInsuficienteException;
import br.com.triboraizes.produtosapi.exception.ProdutoNaoEncontradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.MovimentacaoEstoque;
import br.com.triboraizes.produtosapi.model.Produto;
import br.com.triboraizes.produtosapi.model.TipoMovimentacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimentacaoServiceTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private DAOFactory daoFactory;

    @Mock
    private ProdutoDAO produtoDAO;

    @Mock
    private MovimentacaoDAO movimentacaoDAO;

    private MovimentacaoService movimentacaoService;

    private Produto produto;

    @BeforeEach
    void setUp() {
        when(daoFactory.criarProdutoDAO()).thenReturn(produtoDAO);
        when(daoFactory.criarMovimentacaoDAO()).thenReturn(movimentacaoDAO);
        movimentacaoService = new MovimentacaoService(daoFactory);

        produto = new Produto("Teclado", null, new BigDecimal("349.90"), 10);
        ReflectionTestUtils.setField(produto, "id", ID);
    }

    @Test
    @DisplayName("Deve registrar uma entrada somando ao estoque")
    void deveRegistrarEntrada() {
        // given
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(produto));
        when(movimentacaoDAO.salvar(any(MovimentacaoEstoque.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        MovimentacaoResponseDTO response = movimentacaoService.registrar(ID,
                new MovimentacaoRequestDTO(TipoMovimentacao.ENTRADA, 5, " Reposição "));

        // then
        assertThat(produto.getQuantidadeEmEstoque()).isEqualTo(15);
        assertThat(response)
                .extracting(MovimentacaoResponseDTO::produtoId, MovimentacaoResponseDTO::tipo,
                        MovimentacaoResponseDTO::saldoAnterior, MovimentacaoResponseDTO::saldoApos,
                        MovimentacaoResponseDTO::motivo)
                .containsExactly(ID, TipoMovimentacao.ENTRADA, 10, 15, "Reposição");
        verify(produtoDAO).atualizar(produto);
    }

    @Test
    @DisplayName("Deve registrar uma saída subtraindo do estoque")
    void deveRegistrarSaida() {
        // given
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(produto));
        when(movimentacaoDAO.salvar(any(MovimentacaoEstoque.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        MovimentacaoResponseDTO response = movimentacaoService.registrar(ID,
                new MovimentacaoRequestDTO(TipoMovimentacao.SAIDA, 10, null));

        // then
        assertThat(produto.getQuantidadeEmEstoque()).isZero();
        assertThat(response.saldoApos()).isZero();
    }

    @Test
    @DisplayName("Deve recusar uma saída maior que o estoque sem alterar o produto")
    void deveRecusarSaidaMaiorQueEstoque() {
        // given
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(produto));

        // when / then
        assertThatThrownBy(() -> movimentacaoService.registrar(ID,
                new MovimentacaoRequestDTO(TipoMovimentacao.SAIDA, 11, null)))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("10");
        assertThat(produto.getQuantidadeEmEstoque()).isEqualTo(10);
        verify(produtoDAO, never()).atualizar(any());
        verify(movimentacaoDAO, never()).salvar(any());
    }

    @Test
    @DisplayName("Deve recusar o registro manual de um ajuste")
    void deveRecusarAjusteManual() {
        // given
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(produto));

        // when / then
        assertThatThrownBy(() -> movimentacaoService.registrar(ID,
                new MovimentacaoRequestDTO(TipoMovimentacao.AJUSTE, 1, null)))
                .isInstanceOf(RequisicaoInvalidaException.class);
        verify(movimentacaoDAO, never()).salvar(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao movimentar um produto inexistente")
    void deveLancarExcecaoParaProdutoInexistente() {
        // given
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> movimentacaoService.registrar(ID,
                new MovimentacaoRequestDTO(TipoMovimentacao.ENTRADA, 1, null)))
                .isInstanceOf(ProdutoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve listar uma página do histórico do produto")
    void deveListarHistorico() {
        // given
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(produto));
        MovimentacaoEstoque entrada = new MovimentacaoEstoque(produto, TipoMovimentacao.ENTRADA, 10, 0, null);
        when(movimentacaoDAO.listarPorProduto(produto, 0, 20)).thenReturn(List.of(entrada));
        when(movimentacaoDAO.contarPorProduto(produto)).thenReturn(1L);

        // when
        PaginaDTO<MovimentacaoResponseDTO> pagina = movimentacaoService.listar(ID, 0, 20);

        // then
        assertThat(pagina.conteudo()).extracting(MovimentacaoResponseDTO::tipo).containsExactly(TipoMovimentacao.ENTRADA);
        assertThat(pagina.totalElementos()).isEqualTo(1);
    }
}
