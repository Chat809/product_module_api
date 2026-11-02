package br.com.triboraizes.produtosapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_em_estoque", nullable = false)
    private Integer quantidadeEmEstoque;

    protected Produto() {
    }

    public Produto(String nome, String descricao, BigDecimal preco, Integer quantidadeEmEstoque) {
        atualizar(nome, descricao, preco, quantidadeEmEstoque);
    }

    public void atualizar(String nome, String descricao, BigDecimal preco, Integer quantidadeEmEstoque) {
        this.nome = strip(nome);
        this.descricao = strip(descricao);
        this.preco = preco == null ? null : preco.setScale(2, RoundingMode.HALF_UP);
        this.quantidadeEmEstoque = quantidadeEmEstoque;
    }

    private static String strip(String valor) {
        return valor == null ? null : valor.strip();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public Integer getQuantidadeEmEstoque() {
        return quantidadeEmEstoque;
    }
}
