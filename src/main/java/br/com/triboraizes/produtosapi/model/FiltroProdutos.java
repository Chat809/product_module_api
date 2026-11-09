package br.com.triboraizes.produtosapi.model;

/**
 * @param busca     trecho procurado no código, nome, descrição ou categoria; {@code null} não filtra
 * @param ativo     {@code true} só ativos, {@code false} só inativos, {@code null} todos
 * @param categoria categoria exata, sem diferenciar maiúsculas; {@code null} não filtra
 */
public record FiltroProdutos(String busca, Boolean ativo, String categoria) {
}
