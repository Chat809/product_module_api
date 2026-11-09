package br.com.triboraizes.produtosapi.exception;

/** 404 dos cadastros genéricos; o título vira o {@code title} do ProblemDetail ("Cliente não encontrado"). */
public class RegistroNaoEncontradoException extends RuntimeException {

    private final String titulo;

    public RegistroNaoEncontradoException(String titulo, String mensagem) {
        super(mensagem);
        this.titulo = titulo;
    }

    public String titulo() {
        return titulo;
    }
}
