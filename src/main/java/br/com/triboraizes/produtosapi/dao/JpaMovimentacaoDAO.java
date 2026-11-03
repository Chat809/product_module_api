package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.model.MovimentacaoEstoque;
import br.com.triboraizes.produtosapi.model.Produto;
import jakarta.persistence.EntityManager;

import java.util.List;

public class JpaMovimentacaoDAO implements MovimentacaoDAO {

    private final EntityManager entityManager;

    public JpaMovimentacaoDAO(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public MovimentacaoEstoque salvar(MovimentacaoEstoque movimentacao) {
        entityManager.persist(movimentacao);
        entityManager.flush();
        return movimentacao;
    }

    @Override
    public List<MovimentacaoEstoque> listarPorProduto(Produto produto, int pagina, int tamanho) {
        return entityManager.createQuery("""
                        select m from MovimentacaoEstoque m
                        where m.produto = :produto
                        order by m.criadoEm desc, m.id""", MovimentacaoEstoque.class)
                .setParameter("produto", produto)
                .setFirstResult(pagina * tamanho)
                .setMaxResults(tamanho)
                .getResultList();
    }

    @Override
    public long contarPorProduto(Produto produto) {
        return entityManager.createQuery(
                        "select count(m) from MovimentacaoEstoque m where m.produto = :produto", Long.class)
                .setParameter("produto", produto)
                .getSingleResult();
    }

    @Override
    public void removerPorProduto(Produto produto) {
        entityManager.createQuery("delete from MovimentacaoEstoque m where m.produto = :produto")
                .setParameter("produto", produto)
                .executeUpdate();
    }
}
