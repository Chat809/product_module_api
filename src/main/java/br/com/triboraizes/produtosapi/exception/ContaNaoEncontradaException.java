package br.com.triboraizes.produtosapi.exception;

import java.util.UUID;

public class ContaNaoEncontradaException extends RuntimeException {

    public ContaNaoEncontradaException(UUID id) {
        super("Conta com id " + id + " não encontrada");
    }
}
