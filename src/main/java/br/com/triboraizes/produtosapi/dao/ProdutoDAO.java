package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.model.Produto;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de acesso a dados de {@link Produto}, independente da tecnologia de persistência.
 * As operações devem ser executadas dentro de uma transação aberta pela camada de serviço.
 */
public interface ProdutoDAO {

    Produto salvar(Produto produto);

    Optional<Produto> buscarPorId(Long id);

    List<Produto> listarTodos();

    Produto atualizar(Produto produto);

    void remover(Produto produto);
}
