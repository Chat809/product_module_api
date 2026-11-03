package br.com.triboraizes.produtosapi.model;

import br.com.triboraizes.produtosapi.exception.EstoqueInsuficienteException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal preco;

    @Column(name = "quantidade_em_estoque", nullable = false)
    private Integer quantidadeEmEstoque;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long versao;

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

    public void registrarEntrada(int quantidade) {
        exigirPositiva(quantidade);
        this.quantidadeEmEstoque += quantidade;
    }

    public void registrarSaida(int quantidade) {
        exigirPositiva(quantidade);
        if (quantidade > quantidadeEmEstoque) {
            throw new EstoqueInsuficienteException(quantidadeEmEstoque, quantidade);
        }
        this.quantidadeEmEstoque -= quantidade;
    }

    private static void exigirPositiva(int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade movimentada deve ser maior que zero");
        }
    }

    private static String strip(String valor) {
        return valor == null ? null : valor.strip();
    }

    public UUID getId() {
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

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public Long getVersao() {
        return versao;
    }
}
