package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.model.Produto;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaProdutoDAO implements ProdutoDAO {

    private final EntityManager entityManager;

    public JpaProdutoDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Produto salvar(Produto produto) {
        entityManager.persist(produto);
        entityManager.flush();
        return produto;
    }

    @Override
    public Optional<Produto> buscarPorId(UUID id) {
        return Optional.ofNullable(entityManager.find(Produto.class, id));
    }

    @Override
    public List<Produto> listarTodos() {
        return entityManager
                .createQuery("select p from Produto p order by p.nome, p.id", Produto.class)
                .getResultList();
    }

    @Override
    public Produto atualizar(Produto produto) {
        Produto gerenciado = entityManager.merge(produto);
        entityManager.flush();
        return gerenciado;
    }

    @Override
    public void remover(Produto produto) {
        entityManager.remove(entityManager.contains(produto) ? produto : entityManager.merge(produto));
    }
}
