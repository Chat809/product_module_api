package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.MovimentacaoDAO;
import br.com.triboraizes.produtosapi.dao.ProdutoDAO;
import br.com.triboraizes.produtosapi.dto.MovimentacaoRequestDTO;
import br.com.triboraizes.produtosapi.dto.MovimentacaoResponseDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.exception.ProdutoNaoEncontradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.MovimentacaoEstoque;
import br.com.triboraizes.produtosapi.model.Produto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MovimentacaoService {

    public static final int TAMANHO_MAXIMO_PAGINA = 100;

    private static final Logger log = LoggerFactory.getLogger(MovimentacaoService.class);

    private final ProdutoDAO produtoDAO;
    private final MovimentacaoDAO movimentacaoDAO;

    public MovimentacaoService(DAOFactory daoFactory) {
        this.produtoDAO = daoFactory.criarProdutoDAO();
        this.movimentacaoDAO = daoFactory.criarMovimentacaoDAO();
    }

    @Transactional
    public MovimentacaoResponseDTO registrar(UUID produtoId, MovimentacaoRequestDTO request) {
        Produto produto = buscarProduto(produtoId);
        int saldoAnterior = produto.getQuantidadeEmEstoque();

        switch (request.tipo()) {
            case ENTRADA -> produto.registrarEntrada(request.quantidade());
            case SAIDA -> produto.registrarSaida(request.quantidade());
            case AJUSTE -> throw new RequisicaoInvalidaException(
                    "Use ENTRADA ou SAIDA. Ajustes são registrados automaticamente ao editar o produto");
        }
        produtoDAO.atualizar(produto);
        MovimentacaoEstoque movimentacao = movimentacaoDAO.salvar(new MovimentacaoEstoque(
                produto, request.tipo(), request.quantidade(), saldoAnterior, request.motivo()));

        log.info("Movimentação registrada: produto={}, tipo={}, quantidade={}, saldo={}",
                produtoId, request.tipo(), request.quantidade(), produto.getQuantidadeEmEstoque());
        return MovimentacaoResponseDTO.from(movimentacao);
    }

    public PaginaDTO<MovimentacaoResponseDTO> listar(UUID produtoId, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new RequisicaoInvalidaException(
                    "Use 'page' a partir de 0 e 'size' entre 1 e " + TAMANHO_MAXIMO_PAGINA);
        }
        Produto produto = buscarProduto(produtoId);
        return PaginaDTO.of(
                movimentacaoDAO.listarPorProduto(produto, pagina, tamanho),
                MovimentacaoResponseDTO::from,
                pagina,
                tamanho,
                movimentacaoDAO.contarPorProduto(produto));
    }

    private Produto buscarProduto(UUID id) {
        return produtoDAO.buscarPorId(id).orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }
}
