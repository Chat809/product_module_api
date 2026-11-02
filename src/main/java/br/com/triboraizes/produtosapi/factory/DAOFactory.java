package br.com.triboraizes.produtosapi.factory;

import br.com.triboraizes.produtosapi.dao.JpaProdutoDAO;
import br.com.triboraizes.produtosapi.dao.ProdutoDAO;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

/**
 * Ponto único de criação dos DAOs. Quem consome conhece apenas as interfaces,
 * então trocar a tecnologia de persistência exige alterar somente esta classe.
 */
@Component
public class DAOFactory {

    private final EntityManager entityManager;

    public DAOFactory(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public ProdutoDAO criarProdutoDAO() {
        return new JpaProdutoDAO(entityManager);
    }
}
