package br.com.triboraizes.produtosapi.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Linha de {@code reclamacoes}. O protocolo é gerado pelo banco; {@code cliente} traz o nome do cliente
 * cadastrado, e {@code nomeContato} identifica quem reclamou quando não há cadastro.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Reclamacao(
        UUID id,
        String protocolo,
        @JsonAlias("cliente_id") UUID clienteId,
        ClienteResumo cliente,
        @JsonAlias("nome_contato") String nomeContato,
        String contato,
        String assunto,
        String descricao,
        String canal,
        String prioridade,
        String status,
        @JsonAlias("responsavel_id") UUID responsavelId,
        LocalDate prazo,
        String solucao,
        @JsonAlias("resolvida_em") OffsetDateTime resolvidaEm,
        @JsonAlias("criado_em") OffsetDateTime criadoEm,
        @JsonAlias("atualizado_em") OffsetDateTime atualizadoEm
) {
    public static final String COLUNAS = "*,cliente:clientes(id,nome)";

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ClienteResumo(UUID id, String nome) {
    }
}
