package br.com.triboraizes.produtosapi.client;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Descrição de uma falha do Supabase segura para o log. A mensagem das exceções do RestClient traz o corpo
 * da resposta e a URL chamada (com filtros que podem conter e-mails), então só o status ou o tipo é registrado.
 */
public final class FalhaDoSupabase {

    private FalhaDoSupabase() {
    }

    public static String descrever(RestClientException ex) {
        if (ex instanceof RestClientResponseException resposta) {
            return "HTTP " + resposta.getStatusCode().value();
        }
        return ex.getClass().getSimpleName();
    }
}
