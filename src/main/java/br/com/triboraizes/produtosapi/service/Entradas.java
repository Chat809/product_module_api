package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;

import java.util.Locale;

/** Normalização dos textos recebidos e validação de paginação, comuns aos cadastros. */
final class Entradas {

    static final int TAMANHO_MAXIMO_PAGINA = 100;

    private Entradas() {
    }

    /** Texto aparado, ou {@code null} quando vazio. */
    static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    /** Só os dígitos (CPF, CNPJ, telefone), ou {@code null} quando não sobra nenhum. */
    static String digitos(String valor) {
        String digitos = valor == null ? "" : valor.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }

    static String maiusculo(String valor) {
        String texto = opcional(valor);
        return texto == null ? null : texto.toUpperCase(Locale.ROOT);
    }

    static String minusculo(String valor) {
        String texto = opcional(valor);
        return texto == null ? null : texto.toLowerCase(Locale.ROOT);
    }

    static void validarPagina(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new RequisicaoInvalidaException("Use 'page' a partir de 0 e 'size' entre 1 e " + TAMANHO_MAXIMO_PAGINA);
        }
    }

    /** Remove o que tem significado na sintaxe de filtros do PostgREST, para o texto digitado não alterar a busca. */
    static String termo(String busca) {
        return busca == null ? "" : busca.replaceAll("[,()*\"\\\\%:.]", " ").strip();
    }

    /** Grupo {@code or} do PostgREST que procura o termo em várias colunas. */
    static String buscaEm(String termo, String... colunas) {
        StringBuilder grupo = new StringBuilder("(");
        for (int i = 0; i < colunas.length; i++) {
            grupo.append(i == 0 ? "" : ",").append(colunas[i]).append(".ilike.*").append(termo).append('*');
        }
        return grupo.append(')').toString();
    }
}
