package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.ProdutoDAO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoRequestDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoResponseDTO;
import br.com.triboraizes.produtosapi.exception.ConflitoDeVersaoException;
import br.com.triboraizes.produtosapi.exception.ProdutoNaoEncontradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID INEXISTENTE = UUID.fromString("99999999-9999-9999-9999-999999999999");

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
        ProdutoRequestDTO request = new ProdutoRequestDTO("  Teclado  ", "Mecânico", new BigDecimal("349.90"), 25, null);
        when(produtoDAO.salvar(any(Produto.class))).thenAnswer(invocation -> {
            Produto produto = invocation.getArgument(0);
            ReflectionTestUtils.setField(produto, "id", ID);
            return produto;
        });

        // when
        ProdutoResponseDTO response = produtoService.criar(request);

        // then
        assertThat(response).extracting(ProdutoResponseDTO::id, ProdutoResponseDTO::nome, ProdutoResponseDTO::descricao, ProdutoResponseDTO::preco, ProdutoResponseDTO::quantidadeEmEstoque)
                .containsExactly(ID, "Teclado", "Mecânico", new BigDecimal("349.90"), 25);
        ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
        verify(produtoDAO).salvar(captor.capture());
        assertThat(captor.getValue().getNome()).isEqualTo("Teclado");
    }

    @Test
    @DisplayName("Deve atualizar todos os dados de um produto existente, normalizando o preço para 2 casas")
    void deveAtualizarProdutoExistente() {
        // given
        Produto existente = produtoComId(ID, "Teclado", "Antigo", "100.00", 5);
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(existente));
        when(produtoDAO.atualizar(existente)).thenReturn(existente);
        ProdutoRequestDTO request = new ProdutoRequestDTO("Mouse", "Sem fio", new BigDecimal("89.9"), 10, null);

        // when
        ProdutoResponseDTO response = produtoService.atualizar(ID, request);

        // then
        assertThat(response).extracting(ProdutoResponseDTO::id, ProdutoResponseDTO::nome, ProdutoResponseDTO::descricao, ProdutoResponseDTO::preco, ProdutoResponseDTO::quantidadeEmEstoque)
                .containsExactly(ID, "Mouse", "Sem fio", new BigDecimal("89.90"), 10);
        verify(produtoDAO).atualizar(existente);
    }

    @Test
    @DisplayName("Deve atualizar quando a versão enviada for a versão atual do produto")
    void deveAtualizarQuandoVersaoConferir() {
        // given
        Produto existente = produtoComId(ID, "Teclado", null, "100.00", 5);
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(existente));
        when(produtoDAO.atualizar(existente)).thenReturn(existente);

        // when
        produtoService.atualizar(ID, new ProdutoRequestDTO("Mouse", null, new BigDecimal("89.90"), 10, 0L));

        // then
        verify(produtoDAO).atualizar(existente);
    }

    @Test
    @DisplayName("Deve recusar a atualização quando o produto já tiver sido alterado por outra pessoa")
    void deveRecusarAtualizacaoComVersaoDesatualizada() {
        // given
        Produto existente = produtoComId(ID, "Teclado", null, "100.00", 5);
        ReflectionTestUtils.setField(existente, "versao", 3L);
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(existente));
        ProdutoRequestDTO request = new ProdutoRequestDTO("Mouse", null, new BigDecimal("89.90"), 10, 2L);

        // when / then
        assertThatThrownBy(() -> produtoService.atualizar(ID, request))
                .isInstanceOf(ConflitoDeVersaoException.class);
        assertThat(existente.getNome()).isEqualTo("Teclado");
        verify(produtoDAO, never()).atualizar(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar um produto inexistente")
    void deveLancarExcecaoAoAtualizarProdutoInexistente() {
        // given
        when(produtoDAO.buscarPorId(INEXISTENTE)).thenReturn(Optional.empty());
        ProdutoRequestDTO request = new ProdutoRequestDTO("Mouse", null, new BigDecimal("89.90"), 10, null);

        // when / then
        assertThatThrownBy(() -> produtoService.atualizar(INEXISTENTE, request))
                .isInstanceOf(ProdutoNaoEncontradoException.class)
                .hasMessageContaining(INEXISTENTE.toString());
        verify(produtoDAO, never()).atualizar(any());
    }

    @Test
    @DisplayName("Deve remover um produto existente")
    void deveRemoverProdutoExistente() {
        // given
        Produto existente = produtoComId(ID, "Teclado", null, "100.00", 5);
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(existente));

        // when
        produtoService.remover(ID);

        // then
        verify(produtoDAO).remover(existente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao remover um produto inexistente")
    void deveLancarExcecaoAoRemoverProdutoInexistente() {
        // given
        when(produtoDAO.buscarPorId(INEXISTENTE)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> produtoService.remover(INEXISTENTE))
                .isInstanceOf(ProdutoNaoEncontradoException.class);
        verify(produtoDAO, never()).remover(any());
    }

    @Test
    @DisplayName("Deve retornar o produto quando o id existir")
    void deveBuscarProdutoPorId() {
        // given
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(produtoComId(ID, "Teclado", "Mecânico", "349.90", 25)));

        // when
        ProdutoResponseDTO response = produtoService.buscarPorId(ID);

        // then
        assertThat(response.id()).isEqualTo(ID);
        assertThat(response.nome()).isEqualTo("Teclado");
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar um produto inexistente")
    void deveLancarExcecaoAoBuscarProdutoInexistente() {
        // given
        when(produtoDAO.buscarPorId(INEXISTENTE)).thenReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> produtoService.buscarPorId(INEXISTENTE))
                .isInstanceOf(ProdutoNaoEncontradoException.class)
                .hasMessage("Produto com id " + INEXISTENTE + " não encontrado");
    }

    @Test
    @DisplayName("Deve listar uma página de produtos com os totais calculados")
    void deveListarPaginaDeProdutos() {
        // given
        when(produtoDAO.listar("tec", 1, 2, "preco", false)).thenReturn(List.of(
                produtoComId(ID, "Teclado", null, "349.90", 25),
                produtoComId(ID_2, "Teclado gamer", null, "89.90", 10)));
        when(produtoDAO.contar("tec")).thenReturn(5L);

        // when
        PaginaDTO<ProdutoResponseDTO> pagina = produtoService.listar("  tec ", 1, 2, "preco,DESC");

        // then
        assertThat(pagina.conteudo()).extracting(ProdutoResponseDTO::id).containsExactly(ID, ID_2);
        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.totalElementos()).isEqualTo(5);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve listar todos os produtos quando a busca estiver em branco")
    void deveIgnorarBuscaEmBranco() {
        // given
        when(produtoDAO.listar(null, 0, 20, "nome", true)).thenReturn(List.of());
        when(produtoDAO.contar(null)).thenReturn(0L);

        // when
        PaginaDTO<ProdutoResponseDTO> pagina = produtoService.listar(" ", 0, 20, "nome");

        // then
        assertThat(pagina.conteudo()).isEmpty();
        assertThat(pagina.totalPaginas()).isZero();
    }

    @ParameterizedTest(name = "page={0}, size={1}, sort={2}")
    @CsvSource(delimiter = ';', value = {"-1;20;nome,asc", "0;0;nome,asc", "0;501;nome,asc", "0;20;senha,asc", "0;20;nome,cima"})
    @DisplayName("Deve recusar parâmetros de listagem inválidos sem consultar o banco")
    void deveRecusarParametrosDeListagemInvalidos(int pagina, int tamanho, String ordenacao) {
        assertThatThrownBy(() -> produtoService.listar(null, pagina, tamanho, ordenacao))
                .isInstanceOf(RequisicaoInvalidaException.class);
        verifyNoInteractions(produtoDAO);
    }

    private static Produto produtoComId(UUID id, String nome, String descricao, String preco, int quantidade) {
        Produto produto = new Produto(nome, descricao, new BigDecimal(preco), quantidade);
        ReflectionTestUtils.setField(produto, "id", id);
        ReflectionTestUtils.setField(produto, "versao", 0L);
        return produto;
    }
}
