package br.com.triboraizes.produtosapi.dao;

import br.com.triboraizes.produtosapi.model.Conta;
import br.com.triboraizes.produtosapi.model.FiltroContas;
import br.com.triboraizes.produtosapi.model.ResumoFinanceiro;
import br.com.triboraizes.produtosapi.model.StatusConta;
import br.com.triboraizes.produtosapi.model.TipoConta;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Contrato de acesso às contas a pagar e a receber; o {@link TipoConta} escolhe a tabela. */
public interface ContaDAO {

    record Pagina(List<Conta> contas, long total) {
    }

    /** Uma página ordenada por vencimento, com o total de contas que atendem ao filtro. */
    Pagina listar(TipoConta tipo, FiltroContas filtro, int pagina, int tamanho);

    Optional<Conta> buscarPorId(TipoConta tipo, UUID id);

    Conta criar(TipoConta tipo, Conta conta);

    /** Grava os dados do lançamento; status e data de baixa só mudam por {@link #alterarStatus}. */
    Optional<Conta> atualizar(TipoConta tipo, UUID id, Conta conta);

    Optional<Conta> alterarStatus(TipoConta tipo, UUID id, StatusConta status, LocalDate dataBaixa);

    boolean excluir(TipoConta tipo, UUID id);

    ResumoFinanceiro resumo(LocalDate hoje);
}
