package br.com.triboraizes.produtosapi.model;

import java.util.UUID;

/** Filtros opcionais da listagem de demandas; {@code null} significa "sem filtro". */
public record FiltroDemandas(UUID responsavel, StatusDemanda status, String busca, boolean somenteAbertas) {
}
