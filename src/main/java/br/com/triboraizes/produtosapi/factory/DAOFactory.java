package br.com.triboraizes.produtosapi.factory;

import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.dao.MovimentacaoDAO;
import br.com.triboraizes.produtosapi.dao.ProdutoDAO;
import br.com.triboraizes.produtosapi.dao.SupabaseMovimentacaoDAO;
import br.com.triboraizes.produtosapi.dao.SupabaseProdutoDAO;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Ponto único de criação dos DAOs de produtos e estoque. Quem consome conhece apenas as interfaces,
 * então trocar a tecnologia de persistência exige alterar somente esta classe.
 */
@Component
public class DAOFactory {

    private final RestClient supabaseRestClient;
    private final SupabaseProperties properties;

    public DAOFactory(RestClient supabaseRestClient, SupabaseProperties properties) {
        this.supabaseRestClient = supabaseRestClient;
        this.properties = properties;
    }

    public ProdutoDAO criarProdutoDAO() {
        return new SupabaseProdutoDAO(supabaseRestClient, properties);
    }

    public MovimentacaoDAO criarMovimentacaoDAO() {
        return new SupabaseMovimentacaoDAO(supabaseRestClient, properties);
    }
}
