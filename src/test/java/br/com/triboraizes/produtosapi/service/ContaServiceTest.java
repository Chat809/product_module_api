package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.ContaDAO;
import br.com.triboraizes.produtosapi.dto.ContaDTO;
import br.com.triboraizes.produtosapi.dto.ContaRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.ResumoFinanceiroDTO;
import br.com.triboraizes.produtosapi.exception.ContaNaoEncontradaException;
import br.com.triboraizes.produtosapi.exception.OperacaoNaoPermitidaException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.model.Conta;
import br.com.triboraizes.produtosapi.model.FiltroContas;
import br.com.triboraizes.produtosapi.model.FormaPagamento;
import br.com.triboraizes.produtosapi.model.ResumoFinanceiro;
import br.com.triboraizes.produtosapi.model.StatusConta;
import br.com.triboraizes.produtosapi.model.TipoConta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContaServiceTest {

    private static final UUID ID = UUID.fromString("8c3e4d2a-6f5b-4c7d-8e9f-2a3b4c5d6e7f");
    private static final UUID MARIA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    /** 01:00 UTC do dia 4 ainda é dia 3 em Brasília. */
    private static final LocalDate HOJE = LocalDate.parse("2026-11-03");

    @Mock
    private ContaDAO contaDAO;

    private ContaService service;

    @BeforeEach
    void configurar() {
        service = new ContaService(contaDAO, Clock.fixed(Instant.parse("2026-11-04T01:00:00Z"), ZoneId.of("America/Sao_Paulo")));
    }

    private static Conta conta(StatusConta status, LocalDate vencimento) {
        return new Conta(ID, "Aluguel", "Imobiliária", null, null, new BigDecimal("100.00"), new BigDecimal("10.00"),
                new BigDecimal("10.00"), new BigDecimal("90.00"), vencimento, null, status, FormaPagamento.BOLETO,
                null, null, MARIA, null, null);
    }

    private static ContaRequestDTO pedido(FormaPagamento forma, String chavePix) {
        return new ContaRequestDTO(" Aluguel ", " Imobiliária ", " ", null, new BigDecimal("100.00"), null,
                LocalDate.parse("2026-11-10"), forma, chavePix, "");
    }

    @Test
    @DisplayName("Deve listar com o dia de hoje em Brasília e marcar só pendentes vencidas")
    void deveListar() {
        when(contaDAO.listar(eq(TipoConta.PAGAR), any(), eq(0), eq(20))).thenReturn(new ContaDAO.Pagina(List.of(
                conta(StatusConta.PENDENTE, HOJE.minusDays(1)),
                conta(StatusConta.PENDENTE, HOJE),
                conta(StatusConta.PAGO, HOJE.minusDays(1))), 3));

        PaginaDTO<ContaDTO> pagina = service.listar(TipoConta.PAGAR, null, false, null, null, null, 0, 20);

        assertThat(pagina.conteudo()).extracting(ContaDTO::vencida).containsExactly(true, false, false);
        ArgumentCaptor<FiltroContas> filtro = ArgumentCaptor.forClass(FiltroContas.class);
        verify(contaDAO).listar(eq(TipoConta.PAGAR), filtro.capture(), eq(0), eq(20));
        assertThat(filtro.getValue().hoje()).isEqualTo(HOJE);
    }

    @Test
    @DisplayName("Deve recusar paginação, período invertido e status do outro tipo")
    void deveValidarListagem() {
        assertThatThrownBy(() -> service.listar(TipoConta.PAGAR, null, false, null, null, null, 0, 101))
                .isInstanceOf(RequisicaoInvalidaException.class);
        assertThatThrownBy(() -> service.listar(TipoConta.PAGAR, null, false, null, HOJE, HOJE.minusDays(1), 0, 20))
                .hasMessageContaining("vencimentoDe");
        assertThatThrownBy(() -> service.listar(TipoConta.PAGAR, StatusConta.RECEBIDO, false, null, null, null, 0, 20))
                .hasMessageContaining("PAGO");
        verifyNoInteractions(contaDAO);
    }

    @Test
    @DisplayName("Sem Pix, a chave é descartada; imposto vazio vira 0 e textos são limpos")
    void deveCriarSemPix() {
        when(contaDAO.criar(eq(TipoConta.PAGAR), any())).thenReturn(conta(StatusConta.PENDENTE, HOJE));

        service.criar(TipoConta.PAGAR, pedido(FormaPagamento.BOLETO, "chave-esquecida"), MARIA);

        ArgumentCaptor<Conta> gravada = ArgumentCaptor.forClass(Conta.class);
        verify(contaDAO).criar(eq(TipoConta.PAGAR), gravada.capture());
        assertThat(gravada.getValue().chavePix()).isNull();
        assertThat(gravada.getValue().percentualImposto()).isEqualByComparingTo("0");
        assertThat(gravada.getValue().descricao()).isEqualTo("Aluguel");
        assertThat(gravada.getValue().documento()).isNull();
        assertThat(gravada.getValue().criadoPor()).isEqualTo(MARIA);
    }

    @Test
    @DisplayName("Pix exige chave")
    void deveExigirChavePix() {
        assertThatThrownBy(() -> service.criar(TipoConta.RECEBER, pedido(FormaPagamento.PIX, "  "), MARIA))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessageContaining("chave Pix");
        verify(contaDAO, never()).criar(any(), any());
    }

    @Test
    @DisplayName("Editar mantém a chave quando é Pix; id inexistente vira 404")
    void deveAtualizar() {
        when(contaDAO.atualizar(eq(TipoConta.PAGAR), eq(ID), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(TipoConta.PAGAR, ID, pedido(FormaPagamento.PIX, " pix@x.com ")))
                .isInstanceOf(ContaNaoEncontradaException.class);
        ArgumentCaptor<Conta> gravada = ArgumentCaptor.forClass(Conta.class);
        verify(contaDAO).atualizar(eq(TipoConta.PAGAR), eq(ID), gravada.capture());
        assertThat(gravada.getValue().chavePix()).isEqualTo("pix@x.com");
    }

    @Test
    @DisplayName("Baixa usa hoje por padrão e o status de quitação do tipo")
    void deveBaixar() {
        when(contaDAO.buscarPorId(TipoConta.RECEBER, ID)).thenReturn(Optional.of(conta(StatusConta.PENDENTE, HOJE)));
        when(contaDAO.alterarStatus(TipoConta.RECEBER, ID, StatusConta.RECEBIDO, HOJE))
                .thenReturn(Optional.of(conta(StatusConta.RECEBIDO, HOJE)));

        assertThat(service.baixar(TipoConta.RECEBER, ID, null, MARIA).status()).isEqualTo(StatusConta.RECEBIDO);
    }

    @Test
    @DisplayName("Baixa recusa data futura e conta que não está pendente")
    void deveValidarBaixa() {
        assertThatThrownBy(() -> service.baixar(TipoConta.PAGAR, ID, HOJE.plusDays(1), MARIA))
                .hasMessageContaining("futura");

        when(contaDAO.buscarPorId(TipoConta.PAGAR, ID)).thenReturn(Optional.of(conta(StatusConta.PAGO, HOJE)));
        assertThatThrownBy(() -> service.baixar(TipoConta.PAGAR, ID, HOJE, MARIA))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
        assertThatThrownBy(() -> service.cancelar(TipoConta.PAGAR, ID, MARIA))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
        verify(contaDAO, never()).alterarStatus(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cancelar limpa a data de baixa")
    void deveCancelar() {
        when(contaDAO.buscarPorId(TipoConta.PAGAR, ID)).thenReturn(Optional.of(conta(StatusConta.PENDENTE, HOJE)));
        when(contaDAO.alterarStatus(TipoConta.PAGAR, ID, StatusConta.CANCELADO, null))
                .thenReturn(Optional.of(conta(StatusConta.CANCELADO, HOJE)));

        assertThat(service.cancelar(TipoConta.PAGAR, ID, MARIA).status()).isEqualTo(StatusConta.CANCELADO);
    }

    @Test
    @DisplayName("Buscar e excluir id inexistente respondem 404")
    void deveLancar404() {
        when(contaDAO.buscarPorId(TipoConta.PAGAR, ID)).thenReturn(Optional.empty());
        when(contaDAO.excluir(TipoConta.PAGAR, ID)).thenReturn(false);

        assertThatThrownBy(() -> service.buscar(TipoConta.PAGAR, ID)).isInstanceOf(ContaNaoEncontradaException.class);
        assertThatThrownBy(() -> service.excluir(TipoConta.PAGAR, ID, MARIA)).isInstanceOf(ContaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("Resumo calcula o saldo previsto")
    void deveResumir() {
        when(contaDAO.resumo(HOJE)).thenReturn(new ResumoFinanceiro(new BigDecimal("300"), BigDecimal.ZERO, 0,
                new BigDecimal("1000"), BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO));

        ResumoFinanceiroDTO resumo = service.resumo();

        assertThat(resumo.saldoPrevisto()).isEqualByComparingTo("700");
    }
}
