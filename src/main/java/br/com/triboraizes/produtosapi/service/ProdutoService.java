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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

    public static final int TAMANHO_MAXIMO_PAGINA = 500;

    private static final Logger log = LoggerFactory.getLogger(ProdutoService.class);

    private final ProdutoDAO produtoDAO;

    public ProdutoService(DAOFactory daoFactory) {
        this.produtoDAO = daoFactory.criarProdutoDAO();
    }

    @Transactional
    public ProdutoResponseDTO criar(ProdutoRequestDTO request) {
        Produto produto = produtoDAO.salvar(request.toEntity());
        log.info("Produto criado: id={}", produto.getId());
        return ProdutoResponseDTO.from(produto);
    }

    @Transactional
    public ProdutoResponseDTO atualizar(UUID id, ProdutoRequestDTO request) {
        Produto produto = buscarEntidade(id);
        if (request.versao() != null && !request.versao().equals(produto.getVersao())) {
            throw new ConflitoDeVersaoException(id);
        }
        produto.atualizar(request.nome(), request.descricao(), request.preco(), request.quantidadeEmEstoque());
        Produto atualizado = produtoDAO.atualizar(produto);
        log.info("Produto atualizado: id={}", id);
        return ProdutoResponseDTO.from(atualizado);
    }

    @Transactional
    public void remover(UUID id) {
        produtoDAO.remover(buscarEntidade(id));
        log.info("Produto removido: id={}", id);
    }

    public ProdutoResponseDTO buscarPorId(UUID id) {
        return ProdutoResponseDTO.from(buscarEntidade(id));
    }

    /**
     * @param ordenacao no formato {@code coluna,asc} ou {@code coluna,desc}
     */
    public PaginaDTO<ProdutoResponseDTO> listar(String busca, int pagina, int tamanho, String ordenacao) {
        if (pagina < 0) {
            throw new RequisicaoInvalidaException("O parâmetro 'page' não pode ser negativo");
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new RequisicaoInvalidaException(
                    "O parâmetro 'size' deve estar entre 1 e " + TAMANHO_MAXIMO_PAGINA);
        }
        String[] partes = ordenacao.split(",");
        String coluna = partes[0].strip();
        String direcao = partes.length > 1 ? partes[1].strip().toLowerCase(Locale.ROOT) : "asc";
        if (!ProdutoDAO.COLUNAS_ORDENAVEIS.contains(coluna) || !(direcao.equals("asc") || direcao.equals("desc"))) {
            throw new RequisicaoInvalidaException("O parâmetro 'sort' deve ter o formato coluna,asc|desc, com coluna entre "
                    + ProdutoDAO.COLUNAS_ORDENAVEIS.stream().sorted().toList());
        }
        String termo = busca == null || busca.isBlank() ? null : busca.strip();

        return PaginaDTO.of(
                produtoDAO.listar(termo, pagina, tamanho, coluna, direcao.equals("asc")),
                ProdutoResponseDTO::from,
                pagina,
                tamanho,
                produtoDAO.contar(termo));
    }

    private Produto buscarEntidade(UUID id) {
        return produtoDAO.buscarPorId(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }
}
