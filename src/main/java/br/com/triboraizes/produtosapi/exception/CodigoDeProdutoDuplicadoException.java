package br.com.triboraizes.produtosapi.exception;

public class CodigoDeProdutoDuplicadoException extends RuntimeException {

    public CodigoDeProdutoDuplicadoException(String codigo) {
        super("Já existe um produto com o código " + codigo);
    }
}
