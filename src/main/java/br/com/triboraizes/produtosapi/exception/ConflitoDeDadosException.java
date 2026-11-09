package br.com.triboraizes.produtosapi.exception;

/** 409 de valor único repetido ou de exclusão de registro que outros ainda usam. */
public class ConflitoDeDadosException extends RuntimeException {

    private final String titulo;

    public ConflitoDeDadosException(String titulo, String mensagem) {
        super(mensagem);
        this.titulo = titulo;
    }

    public String titulo() {
        return titulo;
    }
}
