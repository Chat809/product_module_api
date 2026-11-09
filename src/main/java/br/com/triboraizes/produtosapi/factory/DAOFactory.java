package br.com.triboraizes.produtosapi.factory;

import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.dao.CadastroDAO;
import br.com.triboraizes.produtosapi.dao.MovimentacaoDAO;
import br.com.triboraizes.produtosapi.dao.ProdutoDAO;
import br.com.triboraizes.produtosapi.dao.SupabaseCadastroDAO;
import br.com.triboraizes.produtosapi.dao.SupabaseMovimentacaoDAO;
import br.com.triboraizes.produtosapi.dao.SupabaseProdutoDAO;
import br.com.triboraizes.produtosapi.model.CentroCusto;
import br.com.triboraizes.produtosapi.model.Fornecedor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Ponto único de criação dos DAOs de produtos, estoque e cadastros. Quem consome conhece apenas as interfaces,
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

    public CadastroDAO<Fornecedor> criarFornecedorDAO() {
        return cadastro("fornecedores", Fornecedor.COLUNAS, "o fornecedor", Fornecedor.class);
    }

    public CadastroDAO<CentroCusto> criarCentroCustoDAO() {
        return cadastro("centros_custo", CentroCusto.COLUNAS, "o centro de custo", CentroCusto.class);
    }

    private <T> CadastroDAO<T> cadastro(String tabela, String colunas, String nome, Class<T> tipo) {
        return new SupabaseCadastroDAO<>(supabaseRestClient, properties, tabela, colunas, nome, tipo);
    }
}
