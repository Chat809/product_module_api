package br.com.triboraizes.produtosapi.model;

/** Tipos aceitos pela tabela {@code notificacoes}, com a descrição mostrada nas preferências. */
public enum TipoNotificacao {
    LEAD_ATRIBUIDO("Um lead foi atribuído a mim"),
    RECLAMACAO_ABERTA("Nova reclamação sem responsável"),
    RECLAMACAO_ATRIBUIDA("Uma reclamação foi atribuída a mim"),
    DEMANDA_ATRIBUIDA("Fui incluído como responsável em uma demanda"),
    CONTA_VENCENDO("Conta vence nos próximos 3 dias"),
    CONTA_VENCIDA("Conta venceu sem baixa");

    private final String descricao;

    TipoNotificacao(String descricao) {
        this.descricao = descricao;
    }

    public String descricao() {
        return descricao;
    }
}
