package br.com.triboraizes.produtosapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

@Schema(description = "Uma página de resultados")
public record PaginaDTO<T>(
        List<T> conteudo,
        @Schema(description = "Índice da página, começando em 0") int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {

    public static <E, T> PaginaDTO<T> of(List<E> itens, Function<E, T> conversor, int pagina, int tamanho, long total) {
        int totalPaginas = (int) Math.ceil((double) total / tamanho);
        return new PaginaDTO<>(itens.stream().map(conversor).toList(), pagina, tamanho, total, totalPaginas);
    }
}
