package br.com.triboraizes.produtosapi.exception;

/** Corpo da requisição acima do limite aceito (413). */
public class CargaGrandeDemaisException extends RuntimeException {

    public CargaGrandeDemaisException(String mensagem) {
        super(mensagem);
    }
}
