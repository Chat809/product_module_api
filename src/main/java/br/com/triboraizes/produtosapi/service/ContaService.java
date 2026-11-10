package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.ContaDAO;
import br.com.triboraizes.produtosapi.dto.ContaDTO;
import br.com.triboraizes.produtosapi.dto.ContaRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.ResumoFinanceiroDTO;
import br.com.triboraizes.produtosapi.evento.EventoDeNegocio;
import br.com.triboraizes.produtosapi.exception.ContaNaoEncontradaException;
import br.com.triboraizes.produtosapi.exception.OperacaoNaoPermitidaException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.model.Conta;
import br.com.triboraizes.produtosapi.model.FiltroContas;
import br.com.triboraizes.produtosapi.model.FormaPagamento;
import br.com.triboraizes.produtosapi.model.StatusConta;
import br.com.triboraizes.produtosapi.model.TipoConta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Contas a pagar e a receber. O imposto é guardado como percentual e o banco calcula imposto e valor líquido,
 * então os números nunca divergem entre telas, API e relatórios.
 */
@Service
public class ContaService {

    public static final int TAMANHO_MAXIMO_PAGINA = 100;

    private static final Logger log = LoggerFactory.getLogger(ContaService.class);

    private final ContaDAO contaDAO;
    private final Clock relogio;
    private final ApplicationEventPublisher eventos;

    public ContaService(ContaDAO contaDAO, Clock relogio, ApplicationEventPublisher eventos) {
        this.contaDAO = contaDAO;
        this.relogio = relogio;
        this.eventos = eventos;
    }

    public PaginaDTO<ContaDTO> listar(TipoConta tipo, StatusConta status, boolean vencidas, String busca,
                                      LocalDate vencimentoDe, LocalDate vencimentoAte, int pagina, int tamanho) {
        return listar(tipo, status, vencidas, busca, vencimentoDe, vencimentoAte, null, null, pagina, tamanho);
    }

    public PaginaDTO<ContaDTO> listar(TipoConta tipo, StatusConta status, boolean vencidas, String busca,
                                      LocalDate vencimentoDe, LocalDate vencimentoAte, UUID vinculoId,
                                      UUID centroCustoId, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new RequisicaoInvalidaException("Use 'page' a partir de 0 e 'size' entre 1 e " + TAMANHO_MAXIMO_PAGINA);
        }
        if (vencimentoDe != null && vencimentoAte != null && vencimentoDe.isAfter(vencimentoAte)) {
            throw new RequisicaoInvalidaException("'vencimentoDe' deve ser anterior ou igual a 'vencimentoAte'");
        }
        if (status != null) {
            validarStatus(tipo, status);
        }
        LocalDate hoje = hoje();
        ContaDAO.Pagina resultado = contaDAO.listar(tipo,
                new FiltroContas(status, vencidas, busca, vencimentoDe, vencimentoAte, hoje, vinculoId, centroCustoId),
                pagina, tamanho);
        return PaginaDTO.of(resultado.contas(), conta -> ContaDTO.from(conta, hoje), pagina, tamanho, resultado.total());
    }

    public ContaDTO buscar(TipoConta tipo, UUID id) {
        return ContaDTO.from(conta(tipo, id), hoje());
    }

    public ContaDTO criar(TipoConta tipo, ContaRequestDTO request, UUID solicitante) {
        Conta criada = contaDAO.criar(tipo, paraModelo(request, solicitante));
        log.info("Conta lançada: tipo={}, id={}, por={}", tipo, criada.id(), solicitante);
        return ContaDTO.from(criada, hoje());
    }

    public ContaDTO atualizar(TipoConta tipo, UUID id, ContaRequestDTO request) {
        Conta atualizada = contaDAO.atualizar(tipo, id, paraModelo(request, null))
                .orElseThrow(() -> new ContaNaoEncontradaException(id));
        return ContaDTO.from(atualizada, hoje());
    }

    /** Registra o pagamento (a pagar) ou o recebimento (a receber). Só contas pendentes recebem baixa. */
    public ContaDTO baixar(TipoConta tipo, UUID id, LocalDate data, UUID solicitante) {
        LocalDate hoje = hoje();
        LocalDate dataBaixa = data == null ? hoje : data;
        if (dataBaixa.isAfter(hoje)) {
            throw new RequisicaoInvalidaException("A data da baixa não pode ser futura");
        }
        exigirPendente(conta(tipo, id));
        Conta quitada = contaDAO.alterarStatus(tipo, id, tipo.statusQuitado(), dataBaixa)
                .orElseThrow(() -> new ContaNaoEncontradaException(id));
        log.info("Baixa registrada: tipo={}, id={}, data={}, por={}", tipo, id, dataBaixa, solicitante);
        ContaDTO dto = ContaDTO.from(quitada, hoje);
        eventos.publishEvent(new EventoDeNegocio(EventoDeNegocio.CONTA_PAGA, Map.of("tipo", tipo.caminho(), "conta", dto)));
        return dto;
    }

    public ContaDTO cancelar(TipoConta tipo, UUID id, UUID solicitante) {
        exigirPendente(conta(tipo, id));
        Conta cancelada = contaDAO.alterarStatus(tipo, id, StatusConta.CANCELADO, null)
                .orElseThrow(() -> new ContaNaoEncontradaException(id));
        log.info("Conta cancelada: tipo={}, id={}, por={}", tipo, id, solicitante);
        return ContaDTO.from(cancelada, hoje());
    }

    public void excluir(TipoConta tipo, UUID id, UUID solicitante) {
        if (!contaDAO.excluir(tipo, id)) {
            throw new ContaNaoEncontradaException(id);
        }
        log.info("Conta excluída: tipo={}, id={}, por={}", tipo, id, solicitante);
    }

    public ResumoFinanceiroDTO resumo() {
        return ResumoFinanceiroDTO.from(contaDAO.resumo(hoje()));
    }

    /** A chave Pix só é guardada quando a forma é PIX, e nesse caso é obrigatória. */
    private static Conta paraModelo(ContaRequestDTO request, UUID criadoPor) {
        String chavePix = opcional(request.chavePix());
        if (request.formaPagamento() == FormaPagamento.PIX && chavePix == null) {
            throw new RequisicaoInvalidaException("Informe a chave Pix quando a forma de pagamento for PIX");
        }
        if (request.formaPagamento() != FormaPagamento.PIX) {
            chavePix = null;
        }
        BigDecimal percentual = request.percentualImposto() == null ? BigDecimal.ZERO : request.percentualImposto();
        return new Conta(null, request.descricao().trim(), request.pessoa().trim(), opcional(request.documento()),
                opcional(request.categoria()), request.valor(), percentual, null, null, request.vencimento(), null,
                null, request.formaPagamento(), chavePix, opcional(request.observacoes()), criadoPor, null, null,
                request.vinculoId(), request.centroCustoId());
    }

    private static void validarStatus(TipoConta tipo, StatusConta status) {
        boolean quitadoDoOutroTipo = (status == StatusConta.PAGO || status == StatusConta.RECEBIDO)
                && status != tipo.statusQuitado();
        if (quitadoDoOutroTipo) {
            throw new RequisicaoInvalidaException("Use o status " + tipo.statusQuitado() + " para " + tipo.caminho());
        }
    }

    private static void exigirPendente(Conta conta) {
        if (conta.status() != StatusConta.PENDENTE) {
            throw new OperacaoNaoPermitidaException("Só contas pendentes podem receber baixa ou ser canceladas");
        }
    }

    private Conta conta(TipoConta tipo, UUID id) {
        return contaDAO.buscarPorId(tipo, id).orElseThrow(() -> new ContaNaoEncontradaException(id));
    }

    private LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    private static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
