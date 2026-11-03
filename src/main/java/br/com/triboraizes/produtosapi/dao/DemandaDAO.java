package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.model.Demanda;
import br.com.triboraizes.produtosapi.model.FiltroDemandas;
import br.com.triboraizes.produtosapi.model.StatusDemanda;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Contrato de acesso às demandas do Kanban. */
public interface DemandaDAO {

    /** Demandas na ordem do Kanban (posição dentro da coluna). */
    List<Demanda> listar(FiltroDemandas filtro);

    Optional<Demanda> buscarPorId(UUID id);

    /** Grava uma demanda nova no fim da coluna do seu status. */
    Demanda criar(Demanda demanda);

    /** Grava os campos editáveis; ordem, autor e datas não mudam. */
    Optional<Demanda> atualizar(UUID id, Demanda demanda);

    /** Move para a coluna e posição pedidas, reordenando a coluna de destino. */
    Optional<Demanda> mover(UUID id, StatusDemanda status, int posicao);

    boolean excluir(UUID id);
}
