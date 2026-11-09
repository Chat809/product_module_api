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
import br.com.triboraizes.produtosapi.model.TipoMovimentacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MovimentacaoService {

    public static final int TAMANHO_MAXIMO_PAGINA = 100;

    private static final Logger log = LoggerFactory.getLogger(MovimentacaoService.class);

    private final ProdutoDAO produtoDAO;
    private final MovimentacaoDAO movimentacaoDAO;

    public MovimentacaoService(DAOFactory daoFactory) {
        this.produtoDAO = daoFactory.criarProdutoDAO();
        this.movimentacaoDAO = daoFactory.criarMovimentacaoDAO();
    }

    /** O saldo é conferido pelo banco com o produto travado, então duas saídas simultâneas não deixam o estoque negativo. */
    public MovimentacaoResponseDTO registrar(UUID produtoId, MovimentacaoRequestDTO request, UUID usuario) {
        if (request.tipo() == TipoMovimentacao.AJUSTE) {
            throw new RequisicaoInvalidaException(
                    "Use ENTRADA ou SAIDA. Ajustes são registrados automaticamente ao editar o produto");
        }
        String motivo = request.motivo() == null || request.motivo().isBlank() ? null : request.motivo().strip();
        MovimentacaoEstoque movimentacao = movimentacaoDAO
                .registrar(produtoId, request.tipo(), request.quantidade(), motivo, usuario)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(produtoId));

        log.info("Movimentação registrada: produto={}, tipo={}, quantidade={}, saldo={}",
                produtoId, request.tipo(), request.quantidade(), movimentacao.saldoApos());
        return MovimentacaoResponseDTO.from(movimentacao);
    }

    public PaginaDTO<MovimentacaoResponseDTO> listar(UUID produtoId, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new RequisicaoInvalidaException(
                    "Use 'page' a partir de 0 e 'size' entre 1 e " + TAMANHO_MAXIMO_PAGINA);
        }
        if (produtoDAO.buscarPorId(produtoId).isEmpty()) {
            throw new ProdutoNaoEncontradoException(produtoId);
        }
        MovimentacaoDAO.Pagina resultado = movimentacaoDAO.listarPorProduto(produtoId, pagina, tamanho);
        return PaginaDTO.of(resultado.movimentacoes(), MovimentacaoResponseDTO::from, pagina, tamanho, resultado.total());
    }
}
