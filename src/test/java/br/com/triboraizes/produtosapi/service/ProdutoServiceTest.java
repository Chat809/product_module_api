package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.ProdutoDAO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoRequestDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoResponseDTO;
import br.com.triboraizes.produtosapi.dto.ResumoEstoqueDTO;
import br.com.triboraizes.produtosapi.exception.ConflitoDeVersaoException;
import br.com.triboraizes.produtosapi.exception.ProdutoNaoEncontradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.FiltroProdutos;
import br.com.triboraizes.produtosapi.model.Produto;
import br.com.triboraizes.produtosapi.model.ResumoEstoque;
import br.com.triboraizes.produtosapi.model.UnidadeMedida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ID_2 = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID INEXISTENTE = UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID USUARIO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final OffsetDateTime AGORA = OffsetDateTime.parse("2026-11-09T12:00:00Z");
    private static final int LIMITE_ESTOQUE_BAIXO = 5;

    @Mock
    private DAOFactory daoFactory;

    @Mock
    private ProdutoDAO produtoDAO;

    private ProdutoService produtoService;

    @BeforeEach
    void setUp() {
        when(daoFactory.criarProdutoDAO()).thenReturn(produtoDAO);
        produtoService = new ProdutoService(daoFactory, LIMITE_ESTOQUE_BAIXO);
    }

    private static ProdutoRequestDTO request(String codigo, String nome, String preco, int quantidade, Long versao) {
        return new ProdutoRequestDTO(codigo, nome, "Mecânico", " Periféricos ", null, new BigDecimal(preco),
                new BigDecimal("210"), quantidade, null, versao);
    }

    /** O que o banco devolve: os dados gravados com id, datas e versão preenchidos. */
    private static Produto gravado(UUID id, Produto dados, long versao) {
        return new Produto(id, dados.codigo(), dados.nome(), dados.descricao(), dados.categoria(), dados.unidade(),
                dados.preco(), dados.custo(), dados.quantidadeEmEstoque(), dados.ativo(), dados.criadoPor(),
                dados.atualizadoPor(), AGORA, AGORA, versao);
    }

    private static Produto produto(UUID id, String nome) {
        return gravado(id, Produto.novo(null, nome, null, null, null, new BigDecimal("10"), null, 1, null, USUARIO), 0);
    }

    @Test
    @DisplayName("Deve criar o produto já normalizado, com o autor, e devolver o que o banco gravou")
    void deveCriarProduto() {
        when(produtoDAO.criar(any(Produto.class))).thenAnswer(chamada -> gravado(ID, chamada.getArgument(0), 0));

        ProdutoResponseDTO criado = produtoService.criar(request(" per-001 ", "  Teclado  ", "349.9", 25, null), USUARIO);

        ArgumentCaptor<Produto> enviado = ArgumentCaptor.forClass(Produto.class);
        verify(produtoDAO).criar(enviado.capture());
        assertThat(enviado.getValue().codigo()).isEqualTo("PER-001");
        assertThat(enviado.getValue().nome()).isEqualTo("Teclado");
        assertThat(enviado.getValue().categoria()).isEqualTo("Periféricos");
        assertThat(enviado.getValue().unidade()).isEqualTo(UnidadeMedida.UN);
        assertThat(enviado.getValue().ativo()).isTrue();
        assertThat(enviado.getValue().criadoPor()).isEqualTo(USUARIO);
        assertThat(criado.id()).isEqualTo(ID);
        assertThat(criado.preco()).isEqualByComparingTo("349.90");
        assertThat(criado.custo()).isEqualByComparingTo("210.00");
        assertThat(criado.criadoEm()).isEqualTo(AGORA.toInstant());
        assertThat(criado.versao()).isZero();
    }

    @Test
    @DisplayName("Deve atualizar sem checar a versão quando o cliente não informar nenhuma")
    void deveAtualizarSemVersao() {
        when(produtoDAO.atualizar(eq(ID), isNull(), any(Produto.class)))
                .thenAnswer(chamada -> Optional.of(gravado(ID, chamada.getArgument(2), 1)));

        ProdutoResponseDTO atualizado = produtoService.atualizar(ID, request(null, "Teclado gamer", "399.999", 3, null), USUARIO);

        assertThat(atualizado.nome()).isEqualTo("Teclado gamer");
        assertThat(atualizado.preco()).isEqualByComparingTo("400.00");
        assertThat(atualizado.codigo()).isNull();
        assertThat(atualizado.versao()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve repassar a versão lida para o banco só gravar se ela ainda for a atual")
    void deveAtualizarComVersao() {
        when(produtoDAO.atualizar(eq(ID), eq(4L), any(Produto.class)))
                .thenAnswer(chamada -> Optional.of(gravado(ID, chamada.getArgument(2), 5)));

        assertThat(produtoService.atualizar(ID, request(null, "Teclado", "10", 1, 4L), USUARIO).versao()).isEqualTo(5);
    }

    @Test
    @DisplayName("Deve recusar a atualização quando o produto já tiver sido alterado por outra pessoa")
    void deveRecusarVersaoDesatualizada() {
        when(produtoDAO.atualizar(eq(ID), eq(1L), any(Produto.class))).thenReturn(Optional.empty());
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(produto(ID, "Teclado")));

        assertThatThrownBy(() -> produtoService.atualizar(ID, request(null, "Teclado", "10", 1, 1L), USUARIO))
                .isInstanceOf(ConflitoDeVersaoException.class);
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar um produto inexistente")
    void deveFalharAoAtualizarInexistente() {
        when(produtoDAO.atualizar(eq(INEXISTENTE), isNull(), any(Produto.class))).thenReturn(Optional.empty());
        when(produtoDAO.buscarPorId(INEXISTENTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.atualizar(INEXISTENTE, request(null, "Teclado", "10", 1, null), USUARIO))
                .isInstanceOf(ProdutoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve remover um produto existente")
    void deveRemoverProduto() {
        when(produtoDAO.excluir(ID)).thenReturn(true);

        produtoService.remover(ID);

        verify(produtoDAO).excluir(ID);
    }

    @Test
    @DisplayName("Deve lançar exceção ao remover um produto inexistente")
    void deveFalharAoRemoverInexistente() {
        when(produtoDAO.excluir(INEXISTENTE)).thenReturn(false);

        assertThatThrownBy(() -> produtoService.remover(INEXISTENTE))
                .isInstanceOf(ProdutoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve retornar o produto quando o id existir")
    void deveBuscarPorId() {
        when(produtoDAO.buscarPorId(ID)).thenReturn(Optional.of(produto(ID, "Teclado")));

        assertThat(produtoService.buscarPorId(ID).nome()).isEqualTo("Teclado");
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar um produto inexistente")
    void deveFalharAoBuscarInexistente() {
        when(produtoDAO.buscarPorId(INEXISTENTE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.buscarPorId(INEXISTENTE))
                .isInstanceOf(ProdutoNaoEncontradoException.class)
                .hasMessageContaining(INEXISTENTE.toString());
    }

    @Test
    @DisplayName("Deve listar uma página de produtos com os filtros normalizados e os totais calculados")
    void deveListarPaginaDeProdutos() {
        when(produtoDAO.listar(new FiltroProdutos("tec", true, "Periféricos"), 1, 2, "preco", false))
                .thenReturn(new ProdutoDAO.Pagina(List.of(produto(ID, "Teclado"), produto(ID_2, "Teclado gamer")), 5));

        PaginaDTO<ProdutoResponseDTO> pagina = produtoService.listar(
                new FiltroProdutos("  tec ", true, " Periféricos "), 1, 2, "preco,DESC");

        assertThat(pagina.conteudo()).extracting(ProdutoResponseDTO::id).containsExactly(ID, ID_2);
        assertThat(pagina.pagina()).isEqualTo(1);
        assertThat(pagina.totalElementos()).isEqualTo(5);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
    }

    @Test
    @DisplayName("Deve listar todos os produtos quando busca e categoria estiverem em branco")
    void deveIgnorarFiltrosEmBranco() {
        when(produtoDAO.listar(new FiltroProdutos(null, null, null), 0, 20, "nome", true))
                .thenReturn(new ProdutoDAO.Pagina(List.of(), 0));

        PaginaDTO<ProdutoResponseDTO> pagina = produtoService.listar(new FiltroProdutos(" ", null, ""), 0, 20, "nome");

        assertThat(pagina.conteudo()).isEmpty();
        assertThat(pagina.totalPaginas()).isZero();
    }

    @Test
    @DisplayName("Deve devolver as categorias em uso")
    void deveListarCategorias() {
        when(produtoDAO.listarCategorias()).thenReturn(List.of("Acessórios", "Periféricos"));

        assertThat(produtoService.categorias()).containsExactly("Acessórios", "Periféricos");
    }

    @Test
    @DisplayName("Deve montar o resumo do estoque com o limite de estoque baixo configurado")
    void deveMontarResumoDoEstoque() {
        when(produtoDAO.resumir(LIMITE_ESTOQUE_BAIXO))
                .thenReturn(new ResumoEstoque(3, 40, new BigDecimal("1500.00"), 1, 1));

        ResumoEstoqueDTO resumo = produtoService.resumo();

        assertThat(resumo).isEqualTo(new ResumoEstoqueDTO(3, 40, new BigDecimal("1500.00"), 1, 1, LIMITE_ESTOQUE_BAIXO));
    }

    @ParameterizedTest(name = "page={0}, size={1}, sort={2}")
    @CsvSource(delimiter = ';', value = {"-1;20;nome,asc", "0;0;nome,asc", "0;501;nome,asc", "0;20;senha,asc", "0;20;nome,cima"})
    @DisplayName("Deve recusar parâmetros de listagem inválidos sem consultar o banco")
    void deveRecusarParametrosDeListagemInvalidos(int pagina, int tamanho, String ordenacao) {
        assertThatThrownBy(() -> produtoService.listar(new FiltroProdutos(null, null, null), pagina, tamanho, ordenacao))
                .isInstanceOf(RequisicaoInvalidaException.class);
        verifyNoInteractions(produtoDAO);
    }
}
