package br.com.triboraizes.produtosapi.exception;

import java.util.UUID;

public class ConflitoDeVersaoException extends RuntimeException {

    public ConflitoDeVersaoException(UUID id) {
        super("O produto " + id + " foi alterado por outra pessoa. Recarregue os dados e tente novamente.");
    }
}
