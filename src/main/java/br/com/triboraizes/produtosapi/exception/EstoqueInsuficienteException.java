package br.com.triboraizes.produtosapi.exception;

public class EstoqueInsuficienteException extends RuntimeException {

    public EstoqueInsuficienteException(int disponivel, int solicitado) {
        super("Estoque insuficiente: há " + disponivel + " unidade(s) disponível(is) e a saída pedida é de " + solicitado);
    }
}
