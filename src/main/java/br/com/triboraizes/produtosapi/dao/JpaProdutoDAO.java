package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.model.Produto;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class JpaProdutoDAO implements ProdutoDAO {

    private final EntityManager entityManager;

    public JpaProdutoDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Produto salvar(Produto produto) {
        entityManager.persist(produto);
        return produto;
    }

    @Override
    public Optional<Produto> buscarPorId(Long id) {
        return Optional.ofNullable(entityManager.find(Produto.class, id));
    }

    @Override
    public List<Produto> listarTodos() {
        return entityManager
                .createQuery("select p from Produto p order by p.id", Produto.class)
                .getResultList();
    }

    @Override
    public Produto atualizar(Produto produto) {
        return entityManager.merge(produto);
    }

    @Override
    public void remover(Produto produto) {
        entityManager.remove(entityManager.contains(produto) ? produto : entityManager.merge(produto));
    }
}
