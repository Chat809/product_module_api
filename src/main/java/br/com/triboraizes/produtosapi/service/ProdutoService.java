package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.ProdutoDAO;
import br.com.triboraizes.produtosapi.dto.ProdutoRequestDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoResponseDTO;
import br.com.triboraizes.produtosapi.exception.ProdutoNaoEncontradoException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.Produto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

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
    public ProdutoResponseDTO atualizar(Long id, ProdutoRequestDTO request) {
        Produto produto = buscarEntidade(id);
        produto.atualizar(request.nome(), request.descricao(), request.preco(), request.quantidadeEmEstoque());
        Produto atualizado = produtoDAO.atualizar(produto);
        log.info("Produto atualizado: id={}", id);
        return ProdutoResponseDTO.from(atualizado);
    }

    @Transactional
    public void remover(Long id) {
        produtoDAO.remover(buscarEntidade(id));
        log.info("Produto removido: id={}", id);
    }

    public ProdutoResponseDTO buscarPorId(Long id) {
        return ProdutoResponseDTO.from(buscarEntidade(id));
    }

    public List<ProdutoResponseDTO> listarTodos() {
        return produtoDAO.listarTodos().stream()
                .map(ProdutoResponseDTO::from)
                .toList();
    }

    private Produto buscarEntidade(Long id) {
        return produtoDAO.buscarPorId(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }
}
