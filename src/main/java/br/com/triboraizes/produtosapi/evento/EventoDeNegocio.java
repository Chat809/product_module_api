package br.com.triboraizes.produtosapi.evento;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Fato relevante do negócio publicado pelos serviços com o {@code ApplicationEventPublisher}. Os webhooks de saída
 * escutam estes eventos e repassam {@code dados} aos sistemas cadastrados.
 *
 * @param tipo  nome do evento, como {@code lead.criado}; os valores possíveis estão em {@link #TIPOS}
 * @param dados o registro envolvido, serializado como JSON na entrega
 */
public record EventoDeNegocio(String tipo, Object dados, OffsetDateTime ocorridoEm) {

    public static final String CLIENTE_CRIADO = "cliente.criado";
    public static final String LEAD_CRIADO = "lead.criado";
    public static final String LEAD_ETAPA_ALTERADA = "lead.etapa_alterada";
    public static final String LEAD_CONVERTIDO = "lead.convertido";
    public static final String RECLAMACAO_ABERTA = "reclamacao.aberta";
    public static final String RECLAMACAO_STATUS_ALTERADO = "reclamacao.status_alterado";
    public static final String CONTA_PAGA = "conta.paga";
    public static final String PRODUTO_ESTOQUE_BAIXO = "produto.estoque_baixo";

    public static final List<String> TIPOS = List.of(CLIENTE_CRIADO, LEAD_CRIADO, LEAD_ETAPA_ALTERADA,
            LEAD_CONVERTIDO, RECLAMACAO_ABERTA, RECLAMACAO_STATUS_ALTERADO, CONTA_PAGA, PRODUTO_ESTOQUE_BAIXO);

    public EventoDeNegocio(String tipo, Object dados) {
        this(tipo, dados, OffsetDateTime.now());
    }
}
