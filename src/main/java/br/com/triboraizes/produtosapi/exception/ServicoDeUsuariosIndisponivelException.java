package br.com.triboraizes.produtosapi.exception;

public class ServicoDeUsuariosIndisponivelException extends RuntimeException {

    public ServicoDeUsuariosIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
