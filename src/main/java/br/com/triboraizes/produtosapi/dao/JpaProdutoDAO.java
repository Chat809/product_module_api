package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.model.Produto;
import br.com.triboraizes.produtosapi.model.ResumoEstoque;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class JpaProdutoDAO implements ProdutoDAO {

    private static final String FILTRO_BUSCA =
            " where lower(p.nome) like :busca or lower(coalesce(p.descricao, '')) like :busca";

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
    public List<Produto> listar(String busca, int pagina, int tamanho, String ordenarPor, boolean crescente) {
        if (!COLUNAS_ORDENAVEIS.contains(ordenarPor)) {
            throw new IllegalArgumentException("Coluna de ordenação não permitida: " + ordenarPor);
        }
        // A coluna vem de uma lista fechada; o id desempata para a paginação ser estável.
        String jpql = "select p from Produto p" + (busca == null ? "" : FILTRO_BUSCA)
                + " order by p." + ordenarPor + (crescente ? " asc" : " desc") + ", p.id";
        TypedQuery<Produto> query = entityManager.createQuery(jpql, Produto.class)
                .setFirstResult(pagina * tamanho)
                .setMaxResults(tamanho);
        if (busca != null) {
            query.setParameter("busca", padraoDeBusca(busca));
        }
        return query.getResultList();
    }

    @Override
    public long contar(String busca) {
        TypedQuery<Long> query = entityManager.createQuery(
                "select count(p) from Produto p" + (busca == null ? "" : FILTRO_BUSCA), Long.class);
        if (busca != null) {
            query.setParameter("busca", padraoDeBusca(busca));
        }
        return query.getSingleResult();
    }

    @Override
    public ResumoEstoque resumir(int limiteEstoqueBaixo) {
        Object[] linha = entityManager.createQuery("""
                        select count(p),
                               sum(p.quantidadeEmEstoque),
                               sum(p.preco * p.quantidadeEmEstoque),
                               sum(case when p.quantidadeEmEstoque = 0 then 1 else 0 end),
                               sum(case when p.quantidadeEmEstoque between 1 and :limite then 1 else 0 end)
                        from Produto p""", Object[].class)
                .setParameter("limite", limiteEstoqueBaixo)
                .getSingleResult();
        // As somas vêm nulas quando não há produtos.
        return new ResumoEstoque(
                longOuZero(linha[0]),
                longOuZero(linha[1]),
                linha[2] == null ? BigDecimal.ZERO.setScale(2) : new BigDecimal(linha[2].toString()).setScale(2, RoundingMode.HALF_UP),
                longOuZero(linha[3]),
                longOuZero(linha[4]));
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

    private static long longOuZero(Object valor) {
        return valor == null ? 0 : ((Number) valor).longValue();
    }

    private static String padraoDeBusca(String busca) {
        return "%" + busca.toLowerCase(Locale.ROOT) + "%";
    }
}
