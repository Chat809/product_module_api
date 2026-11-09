package br.com.triboraizes.produtosapi.dao;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Operações comuns dos cadastros (fornecedores, clientes, leads, webhooks...). Os dados gravados vão como mapa
 * de coluna para valor; as linhas lidas voltam no tipo do cadastro.
 */
public interface CadastroDAO<T> {

    record Pagina<T>(List<T> itens, long total) {
    }

    /**
     * Filtros no formato do PostgREST (coluna para operador e valor, como {@code ativo -> is.true}), ordenação
     * ({@code nome.asc}) e paginação. A chave {@code or} aceita um grupo, como {@code (nome.ilike.*ana*,email.ilike.*ana*)}.
     */
    record Consulta(Map<String, String> filtros, String ordem, int pagina, int tamanho) {

        public static Consulta de(String ordem, int pagina, int tamanho) {
            return new Consulta(new LinkedHashMap<>(), ordem, pagina, tamanho);
        }

        public Consulta onde(String coluna, String condicao) {
            filtros.put(coluna, condicao);
            return this;
        }
    }

    Pagina<T> listar(Consulta consulta);

    /** Todas as linhas que atendem aos filtros, sem paginação (até 1000), para listas de opções e históricos. */
    List<T> todos(Map<String, String> filtros, String ordem);

    Optional<T> buscar(UUID id);

    T criar(Map<String, Object> dados);

    Optional<T> atualizar(UUID id, Map<String, Object> dados);

    boolean excluir(UUID id);

    /** Atualiza todas as linhas que atendem aos filtros (ao menos um é obrigatório) e devolve quantas mudaram. */
    int atualizarOnde(Map<String, String> filtros, Map<String, Object> dados);

    /** Insere as linhas ou, havendo conflito nas colunas indicadas ({@code usuario_id,tipo}), atualiza as existentes. */
    List<T> gravarVarias(List<Map<String, Object>> linhas, String colunasDoConflito);

    /** Chama uma função do banco ({@code /rpc/nome}) que devolve linhas desta tabela. */
    List<T> rpc(String funcao, Map<String, Object> parametros);
}
