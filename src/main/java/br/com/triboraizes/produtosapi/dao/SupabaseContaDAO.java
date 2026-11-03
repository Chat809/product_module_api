package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.model.Conta;
import br.com.triboraizes.produtosapi.model.FiltroContas;
import br.com.triboraizes.produtosapi.model.ResumoFinanceiro;
import br.com.triboraizes.produtosapi.model.StatusConta;
import br.com.triboraizes.produtosapi.model.TipoConta;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Acessa {@code contas_pagar} e {@code contas_receber} pelo PostgREST com uma única implementação. */
@Repository
public class SupabaseContaDAO extends SupabaseTabela implements ContaDAO {

    private static final ParameterizedTypeReference<List<Conta>> LISTA = new ParameterizedTypeReference<>() {
    };

    public SupabaseContaDAO(RestClient supabaseRestClient, SupabaseProperties properties) {
        super(supabaseRestClient, properties);
    }

    /** O total vem no {@code Content-Range} da mesma resposta (por exemplo {@code 0-19/57}). */
    @Override
    public Pagina listar(TipoConta tipo, FiltroContas filtro, int pagina, int tamanho) {
        StringBuilder uri = new StringBuilder(tabela(tipo) + "?select=" + colunas(tipo)
                + "&order=vencimento.asc,criado_em.asc&offset={offset}&limit={limit}");
        Map<String, Object> valores = new HashMap<>();
        valores.put("offset", (long) pagina * tamanho);
        valores.put("limit", tamanho);
        if (filtro.vencidas()) {
            uri.append("&status=eq.").append(StatusConta.PENDENTE.name()).append("&vencimento=lt.{hoje}");
            valores.put("hoje", filtro.hoje());
        } else if (filtro.status() != null) {
            uri.append("&status=eq.{status}");
            valores.put("status", filtro.status().name());
        }
        if (filtro.vencimentoDe() != null) {
            uri.append("&vencimento=gte.{de}");
            valores.put("de", filtro.vencimentoDe());
        }
        if (filtro.vencimentoAte() != null) {
            uri.append("&vencimento=lte.{ate}");
            valores.put("ate", filtro.vencimentoAte());
        }
        String termo = termoDeBusca(filtro.busca());
        if (!termo.isEmpty()) {
            uri.append("&or={busca}");
            valores.put("busca", "(descricao.ilike.*" + termo + "*," + tipo.colunaPessoa() + ".ilike.*" + termo
                    + "*,categoria.ilike.*" + termo + "*)");
        }
        return chamar(() -> {
            try {
                ResponseEntity<List<Conta>> resposta = restClient.get()
                        .uri(uri.toString(), valores)
                        .header("Prefer", "count=exact")
                        .retrieve()
                        .toEntity(LISTA);
                List<Conta> contas = resposta.getBody() == null ? List.of() : resposta.getBody();
                return new Pagina(contas, total(resposta.getHeaders().getFirst("Content-Range")));
            } catch (HttpClientErrorException ex) {
                // Página além do fim: o PostgREST responde 416 com o total em "*/57".
                if (ex.getStatusCode().value() == 416 && ex.getResponseHeaders() != null) {
                    return new Pagina(List.of(), total(ex.getResponseHeaders().getFirst("Content-Range")));
                }
                throw ex;
            }
        });
    }

    @Override
    public Optional<Conta> buscarPorId(TipoConta tipo, UUID id) {
        return chamar(() -> primeiro(restClient.get()
                .uri(tabela(tipo) + "?select=" + colunas(tipo) + "&id=eq.{id}", id)
                .retrieve()
                .body(LISTA)));
    }

    @Override
    public Conta criar(TipoConta tipo, Conta conta) {
        Map<String, Object> dados = editaveis(tipo, conta);
        dados.put("criado_por", conta.criadoPor());
        return chamar(() -> primeiro(restClient.post()
                .uri(tabela(tipo) + "?select=" + colunas(tipo))
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .body(LISTA))).orElseThrow();
    }

    @Override
    public Optional<Conta> atualizar(TipoConta tipo, UUID id, Conta conta) {
        return gravar(tipo, id, editaveis(tipo, conta));
    }

    @Override
    public Optional<Conta> alterarStatus(TipoConta tipo, UUID id, StatusConta status, LocalDate dataBaixa) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("status", status.name());
        dados.put(tipo.colunaBaixa(), dataBaixa);
        return gravar(tipo, id, dados);
    }

    @Override
    public boolean excluir(TipoConta tipo, UUID id) {
        List<Conta> removidas = chamar(() -> restClient.delete()
                .uri(tabela(tipo) + "?id=eq.{id}&select=id", id)
                .header("Prefer", "return=representation")
                .retrieve()
                .body(LISTA));
        return removidas != null && !removidas.isEmpty();
    }

    @Override
    public ResumoFinanceiro resumo(LocalDate hoje) {
        return chamar(() -> restClient.post()
                .uri("/rest/v1/rpc/resumo_financeiro")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("p_hoje", hoje))
                .retrieve()
                .body(ResumoFinanceiro.class));
    }

    private Optional<Conta> gravar(TipoConta tipo, UUID id, Map<String, Object> dados) {
        return chamar(() -> primeiro(restClient.patch()
                .uri(tabela(tipo) + "?id=eq.{id}&select=" + colunas(tipo), id)
                .header("Prefer", "return=representation")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .body(LISTA)));
    }

    private static Map<String, Object> editaveis(TipoConta tipo, Conta conta) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("descricao", conta.descricao());
        dados.put(tipo.colunaPessoa(), conta.pessoa());
        dados.put("documento", conta.documento());
        dados.put("categoria", conta.categoria());
        dados.put("valor", conta.valor());
        dados.put("percentual_imposto", conta.percentualImposto());
        dados.put("vencimento", conta.vencimento());
        dados.put("forma_pagamento", conta.formaPagamento().name());
        dados.put("chave_pix", conta.chavePix());
        dados.put("observacoes", conta.observacoes());
        return dados;
    }

    private static String tabela(TipoConta tipo) {
        return "/rest/v1/" + tipo.tabela();
    }

    /** Renomeia as colunas que mudam entre as tabelas para que as duas virem o mesmo {@link Conta}. */
    private static String colunas(TipoConta tipo) {
        return "id,descricao,pessoa:" + tipo.colunaPessoa() + ",documento,categoria,valor,percentual_imposto,"
                + "valor_imposto,valor_liquido,vencimento,data_baixa:" + tipo.colunaBaixa()
                + ",status,forma_pagamento,chave_pix,observacoes,criado_por,criado_em,atualizado_em";
    }

    private static long total(String intervalo) {
        if (intervalo == null || !intervalo.contains("/") || intervalo.endsWith("*")) {
            return 0L;
        }
        return Long.parseLong(intervalo.substring(intervalo.indexOf('/') + 1));
    }
}
