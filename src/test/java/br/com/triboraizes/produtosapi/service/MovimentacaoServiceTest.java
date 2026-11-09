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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimentacaoServiceTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USUARIO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-11-09T12:00:00Z");

    @Mock
    private DAOFactory daoFactory;

    @Mock
    private ProdutoDAO produtoDAO;

    @Mock
    private MovimentacaoDAO movimentacaoDAO;

    private MovimentacaoService movimentacaoService;

    @BeforeEach
    void setUp() {
        when(daoFactory.criarProdutoDAO()).thenReturn(produtoDAO);
        when(daoFactory.criarMovimentacaoDAO()).thenReturn(movimentacaoDAO);
        movimentacaoService = new MovimentacaoService(daoFactory);
    }

    private static MovimentacaoEstoque movimentacao(TipoMovimentacao tipo, int quantidade, int antes, int depois,
                                                    String motivo) {
        return new MovimentacaoEstoque(UUID.randomUUID(), ID, tipo, quantidade, antes, depois, motivo, USUARIO, AGORA);
    }

    @Test
    @DisplayName("Deve registrar uma entrada pelo banco, com motivo sem espaços e o autor")
    void deveRegistrarEntrada() {
        when(movimentacaoDAO.registrar(ID, TipoMovimentacao.ENTRADA, 5, "Reposição", USUARIO))
                .thenReturn(Optional.of(movimentacao(TipoMovimentacao.ENTRADA, 5, 10, 15, "Reposição")));

        MovimentacaoResponseDTO registrada = movimentacaoService.registrar(ID,
                new MovimentacaoRequestDTO(TipoMovimentacao.ENTRADA, 5, "  Reposição "), USUARIO);

        assertThat(registrada.produtoId()).isEqualTo(ID);
        assertThat(registrada.saldoAnterior()).isEqualTo(10);
        assertThat(registrada.saldoApos()).isEqualTo(15);
        assertThat(registrada.criadoEm()).isEqualTo(AGORA.toInstant());
    }

    @Test
    @DisplayName("Deve registrar uma saída sem motivo")
    void deveRegistrarSaidaSemMotivo() {
        when(movimentacaoDAO.registrar(ID, TipoMovimentacao.SAIDA, 4, null, USUARIO))
                .thenReturn(Optional.of(movimentacao(TipoMovimentacao.SAIDA, 4, 10, 6, null)));

        MovimentacaoResponseDTO registrada = movimentacaoService.registrar(ID,
                new MovimentacaoRequestDTO(TipoMovimentacao.SAIDA, 4, " "), USUARIO);

        assertThat(registrada.saldoApos()).isEqualTo(6);
        assertThat(registrada.motivo()).isNull();
    }

    @Test
    @DisplayName("Deve repassar a recusa do banco quando a saída for maior que o estoque")
    void deveRecusarSaidaMaiorQueOEstoque() {
        when(movimentacaoDAO.registrar(ID, TipoMovimentacao.SAIDA, 99, null, USUARIO))
                .thenThrow(new EstoqueInsuficienteException(10, 99));

        assertThatThrownBy(() -> movimentacaoService.registrar(ID,
                new MovimentacaoRequestDTO(TipoMovimentacao.SAIDA, 99, null), USUARIO))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("há 10 unidade(s)");
    }

    @Test
    @DisplayName("Deve recusar o registro manual de um ajuste sem chamar o banco")
    void deveRecusarAjusteManual() {
        assertThatThrownBy(() -> movimentacaoService.registrar(ID,
                new MovimentacaoRequestDTO(TipoMovimentacao.AJUSTE, 1, null), USUARIO))
                .isInstanceOf(RequisicaoInvalidaException.class);

        verifyNoInteractions(movimentacaoDAO);
    }

    @Test
    @DisplayName("Deve lançar exceção ao movimentar um produto inexistente")
    void deveFalharComProdutoInexistente() {
        when(movimentacaoDAO.registrar(ID, TipoMovimentacao.ENTRADA, 1, null, USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movimentacaoService.registrar(ID,
                new MovimentacaoRequestDTO(TipoMovimentacao.ENTRADA, 1, null), USUARIO))
                .isInstanceOf(ProdutoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve listar uma página do histórico do produto")
    void deveListarHistorico() {
        Produto produto = new Produto(ID, null, "Teclado", null, null, null, new BigDecimal("10.00"), null, 15, true,
                null, null, AGORA, AGORA, 2L);
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(produto));
        when(movimentacaoDAO.listarPorProduto(ID, 1, 2)).thenReturn(new MovimentacaoDAO.Pagina(
                List.of(movimentacao(TipoMovimentacao.ENTRADA, 5, 10, 15, null)), 3));

        PaginaDTO<MovimentacaoResponseDTO> pagina = movimentacaoService.listar(ID, 1, 2);

        assertThat(pagina.conteudo()).hasSize(1);
        assertThat(pagina.totalElementos()).isEqualTo(3);
        assertThat(pagina.totalPaginas()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve responder 404 no histórico de um produto inexistente e validar a paginação")
    void deveValidarHistorico() {
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> movimentacaoService.listar(ID, 0, 20)).isInstanceOf(ProdutoNaoEncontradoException.class);
        assertThatThrownBy(() -> movimentacaoService.listar(ID, -1, 20)).isInstanceOf(RequisicaoInvalidaException.class);
        assertThatThrownBy(() -> movimentacaoService.listar(ID, 0, 101)).isInstanceOf(RequisicaoInvalidaException.class);
        verify(movimentacaoDAO, never()).listarPorProduto(any(), anyInt(), anyInt());
    }
}
