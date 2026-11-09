package br.com.triboraizes.produtosapi.factory;

import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.dao.CadastroDAO;
import br.com.triboraizes.produtosapi.dao.MovimentacaoDAO;
import br.com.triboraizes.produtosapi.dao.ProdutoDAO;
import br.com.triboraizes.produtosapi.dao.SupabaseCadastroDAO;
import br.com.triboraizes.produtosapi.dao.SupabaseMovimentacaoDAO;
import br.com.triboraizes.produtosapi.dao.SupabaseProdutoDAO;
import br.com.triboraizes.produtosapi.model.CentroCusto;
import br.com.triboraizes.produtosapi.model.Cliente;
import br.com.triboraizes.produtosapi.model.CrmEtapa;
import br.com.triboraizes.produtosapi.model.CrmOrigem;
import br.com.triboraizes.produtosapi.model.Fornecedor;
import br.com.triboraizes.produtosapi.model.Interacao;
import br.com.triboraizes.produtosapi.model.Lead;
import br.com.triboraizes.produtosapi.model.Notificacao;
import br.com.triboraizes.produtosapi.model.PreferenciaNotificacao;
import br.com.triboraizes.produtosapi.model.Reclamacao;
import br.com.triboraizes.produtosapi.model.ReclamacaoEvento;
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

    public CadastroDAO<Cliente> criarClienteDAO() {
        return cadastro("clientes", Cliente.COLUNAS, "o cliente", Cliente.class);
    }

    public CadastroDAO<Lead> criarLeadDAO() {
        return cadastro("leads", Lead.COLUNAS, "o lead", Lead.class);
    }

    public CadastroDAO<CrmEtapa> criarEtapaDAO() {
        return cadastro("crm_etapas", CrmEtapa.COLUNAS, "a etapa", CrmEtapa.class);
    }

    public CadastroDAO<CrmOrigem> criarOrigemDAO() {
        return cadastro("crm_origens", CrmOrigem.COLUNAS, "a origem", CrmOrigem.class);
    }

    public CadastroDAO<Interacao> criarInteracaoDAO() {
        return cadastro("crm_interacoes", Interacao.COLUNAS, "a interação", Interacao.class);
    }

    public CadastroDAO<Reclamacao> criarReclamacaoDAO() {
        return cadastro("reclamacoes", Reclamacao.COLUNAS, "a reclamação", Reclamacao.class);
    }

    public CadastroDAO<ReclamacaoEvento> criarReclamacaoEventoDAO() {
        return cadastro("reclamacao_eventos", ReclamacaoEvento.COLUNAS, "o evento da reclamação", ReclamacaoEvento.class);
    }

    public CadastroDAO<Notificacao> criarNotificacaoDAO() {
        return cadastro("notificacoes", Notificacao.COLUNAS, "a notificação", Notificacao.class);
    }

    public CadastroDAO<PreferenciaNotificacao> criarPreferenciaNotificacaoDAO() {
        return cadastro("preferencias_notificacao", PreferenciaNotificacao.COLUNAS, "a preferência",
                PreferenciaNotificacao.class);
    }

    private <T> CadastroDAO<T> cadastro(String tabela, String colunas, String nome, Class<T> tipo) {
        return new SupabaseCadastroDAO<>(supabaseRestClient, properties, tabela, colunas, nome, tipo);
    }
}
