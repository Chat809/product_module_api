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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Regras do catálogo de produtos. O histórico de estoque ("Estoque inicial" na criação e ajuste quando a
 * quantidade muda na edição) é gravado pelo banco na mesma transação de cada gravação.
 */
@Service
public class ProdutoService {

    public static final int TAMANHO_MAXIMO_PAGINA = 500;

    private static final Logger log = LoggerFactory.getLogger(ProdutoService.class);

    private final ProdutoDAO produtoDAO;
    private final int limiteEstoqueBaixo;

    public ProdutoService(DAOFactory daoFactory, @Value("${app.estoque.limite-baixo:5}") int limiteEstoqueBaixo) {
        this.produtoDAO = daoFactory.criarProdutoDAO();
        this.limiteEstoqueBaixo = limiteEstoqueBaixo;
    }

    public ProdutoResponseDTO criar(ProdutoRequestDTO request, UUID usuario) {
        Produto produto = produtoDAO.criar(request.toProduto(usuario));
        log.info("Produto criado: id={}", produto.id());
        return ProdutoResponseDTO.from(produto);
    }

    /** Com versão informada, só grava se ninguém tiver alterado o produto depois da leitura. */
    public ProdutoResponseDTO atualizar(UUID id, ProdutoRequestDTO request, UUID usuario) {
        Produto atualizado = produtoDAO.atualizar(id, request.versao(), request.toProduto(usuario))
                .orElseThrow(() -> {
                    buscarEntidade(id);
                    return new ConflitoDeVersaoException(id);
                });
        log.info("Produto atualizado: id={}", id);
        return ProdutoResponseDTO.from(atualizado);
    }

    public void remover(UUID id) {
        if (!produtoDAO.excluir(id)) {
            throw new ProdutoNaoEncontradoException(id);
        }
        log.info("Produto removido: id={}", id);
    }

    public ProdutoResponseDTO buscarPorId(UUID id) {
        return ProdutoResponseDTO.from(buscarEntidade(id));
    }

    /**
     * @param ordenacao no formato {@code coluna,asc} ou {@code coluna,desc}
     */
    public PaginaDTO<ProdutoResponseDTO> listar(FiltroProdutos filtro, int pagina, int tamanho, String ordenacao) {
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
        if (!ProdutoDAO.COLUNAS_ORDENAVEIS.containsKey(coluna) || !(direcao.equals("asc") || direcao.equals("desc"))) {
            throw new RequisicaoInvalidaException("O parâmetro 'sort' deve ter o formato coluna,asc|desc, com coluna entre "
                    + ProdutoDAO.COLUNAS_ORDENAVEIS.keySet().stream().sorted().toList());
        }
        FiltroProdutos normalizado = new FiltroProdutos(textoOuNulo(filtro.busca()), filtro.ativo(),
                textoOuNulo(filtro.categoria()));

        ProdutoDAO.Pagina resultado = produtoDAO.listar(normalizado, pagina, tamanho, coluna, direcao.equals("asc"));
        return PaginaDTO.of(resultado.produtos(), ProdutoResponseDTO::from, pagina, tamanho, resultado.total());
    }

    public List<String> categorias() {
        return produtoDAO.listarCategorias();
    }

    public ResumoEstoqueDTO resumo() {
        return ResumoEstoqueDTO.from(produtoDAO.resumir(limiteEstoqueBaixo), limiteEstoqueBaixo);
    }

    private Produto buscarEntidade(UUID id) {
        return produtoDAO.buscarPorId(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    private static String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
