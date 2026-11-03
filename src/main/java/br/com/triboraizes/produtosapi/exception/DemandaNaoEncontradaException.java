package br.com.triboraizes.produtosapi.exception;

import java.util.UUID;

public class DemandaNaoEncontradaException extends RuntimeException {

    public DemandaNaoEncontradaException(UUID id) {
        super("Demanda com id " + id + " não encontrada");
    }
}
