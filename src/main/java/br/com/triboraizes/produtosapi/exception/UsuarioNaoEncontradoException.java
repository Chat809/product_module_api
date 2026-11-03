package br.com.triboraizes.produtosapi.exception;

import java.util.UUID;

public class UsuarioNaoEncontradoException extends RuntimeException {

    public UsuarioNaoEncontradoException(UUID id) {
        super("Usuário com id " + id + " não encontrado");
    }
}
