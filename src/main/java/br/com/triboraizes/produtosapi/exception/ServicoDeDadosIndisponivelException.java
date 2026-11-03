package br.com.triboraizes.produtosapi.exception;

public class ServicoDeDadosIndisponivelException extends RuntimeException {

    public ServicoDeDadosIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
