package br.com.triboraizes.produtosapi.exception;

public class ProdutoNaoEncontradoException extends RuntimeException {

    public ProdutoNaoEncontradoException(Long id) {
        super("Produto com id " + id + " não encontrado");
    }
}
