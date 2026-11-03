package br.com.triboraizes.produtosapi.exception;

import java.util.UUID;

public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(UUID id) {
        super("Produto com id " + id + " não encontrado");
    }
}
