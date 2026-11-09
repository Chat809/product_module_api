package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.exception.EstoqueInsuficienteException;
import br.com.triboraizes.produtosapi.model.MovimentacaoEstoque;
import br.com.triboraizes.produtosapi.model.TipoMovimentacao;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Acessa {@code public.movimentacoes_estoque}. Entradas e saídas passam pela função {@code registrar_movimentacao},
 * que trava o produto, confere o saldo e grava estoque e histórico na mesma transação.
 */
public class SupabaseMovimentacaoDAO extends SupabaseTabela implements MovimentacaoDAO {

    private static final String TABELA = "/rest/v1/movimentacoes_estoque";
    /** A função devolve o saldo do momento em {@code details}. */
    private static final Pattern SALDO = Pattern.compile("\"details\"\\s*:\\s*\"(\\d+)\"");
    private static final ParameterizedTypeReference<List<MovimentacaoEstoque>> LISTA = new ParameterizedTypeReference<>() {
    };

    public SupabaseMovimentacaoDAO(RestClient supabaseRestClient, SupabaseProperties properties) {
        super(supabaseRestClient, properties);
    }

    @Override
    public Optional<MovimentacaoEstoque> registrar(UUID produtoId, TipoMovimentacao tipo, int quantidade, String motivo,
                                                   UUID usuario) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("p_produto_id", produtoId);
        parametros.put("p_tipo", tipo.name());
        parametros.put("p_quantidade", quantidade);
        parametros.put("p_motivo", motivo);
        parametros.put("p_usuario", usuario);
        return chamar(() -> {
            try {
                return primeiro(restClient.post()
                        .uri("/rest/v1/rpc/registrar_movimentacao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(parametros)
                        .retrieve()
                        .body(LISTA));
            } catch (HttpClientErrorException ex) {
                String corpo = ex.getResponseBodyAsString();
                if (corpo.contains("estoque_insuficiente")) {
                    Matcher saldo = SALDO.matcher(corpo);
                    throw new EstoqueInsuficienteException(saldo.find() ? Integer.parseInt(saldo.group(1)) : 0, quantidade);
                }
                throw ex;
            }
        });
    }

    @Override
    public Pagina listarPorProduto(UUID produtoId, int pagina, int tamanho) {
        Resultado<MovimentacaoEstoque> resultado = paginado(restClient.get()
                .uri(TABELA + "?select=*&produto_id=eq.{id}&order=criado_em.desc,id.desc&offset={offset}&limit={limit}",
                        produtoId, (long) pagina * tamanho, tamanho), LISTA);
        return new Pagina(resultado.linhas(), resultado.total());
    }
}
