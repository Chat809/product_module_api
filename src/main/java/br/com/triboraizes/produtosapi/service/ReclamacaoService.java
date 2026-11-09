package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.CadastroDAO;
import br.com.triboraizes.produtosapi.dao.CadastroDAO.Consulta;
import br.com.triboraizes.produtosapi.dto.ComentarioRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.ReclamacaoRequestDTO;
import br.com.triboraizes.produtosapi.dto.StatusReclamacaoRequestDTO;
import br.com.triboraizes.produtosapi.evento.EventoDeNegocio;
import br.com.triboraizes.produtosapi.exception.RegistroNaoEncontradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.Reclamacao;
import br.com.triboraizes.produtosapi.model.ReclamacaoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Reclamações de clientes. O banco gera o protocolo, registra a abertura e cada mudança de status no histórico
 * e marca a data de resolução; aqui ficam as regras de preenchimento e os comentários.
 */
@Service
public class ReclamacaoService {

    private static final Logger log = LoggerFactory.getLogger(ReclamacaoService.class);
    private static final Set<String> STATUS = Set.of("ABERTA", "EM_ANALISE", "RESOLVIDA", "ENCERRADA");
    private static final Set<String> PRIORIDADES = Set.of("BAIXA", "MEDIA", "ALTA", "CRITICA");

    private final CadastroDAO<Reclamacao> reclamacoes;
    private final CadastroDAO<ReclamacaoEvento> eventosDaReclamacao;
    private final ApplicationEventPublisher eventos;

    public ReclamacaoService(DAOFactory fabrica, ApplicationEventPublisher eventos) {
        this.reclamacoes = fabrica.criarReclamacaoDAO();
        this.eventosDaReclamacao = fabrica.criarReclamacaoEventoDAO();
        this.eventos = eventos;
    }

    public PaginaDTO<Reclamacao> listar(String busca, String status, String prioridade, UUID responsavelId,
                                        UUID clienteId, int pagina, int tamanho) {
        Entradas.validarPagina(pagina, tamanho);
        Consulta consulta = Consulta.de("criado_em.desc,id.asc", pagina, tamanho);
        filtrar(consulta.filtros(), busca, responsavelId);
        if (status != null && STATUS.contains(status)) {
            consulta.onde("status", "eq." + status);
        }
        if (prioridade != null && PRIORIDADES.contains(prioridade)) {
            consulta.onde("prioridade", "eq." + prioridade);
        }
        if (clienteId != null) {
            consulta.onde("cliente_id", "eq." + clienteId);
        }
        CadastroDAO.Pagina<Reclamacao> resultado = reclamacoes.listar(consulta);
        return PaginaDTO.of(resultado.itens(), r -> r, pagina, tamanho, resultado.total());
    }

    /** Reclamações do quadro (até 1000), as de prazo mais próximo primeiro; o painel agrupa por status. */
    public List<Reclamacao> quadro(String busca, UUID responsavelId) {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtrar(filtros, busca, responsavelId);
        return reclamacoes.todos(filtros, "prazo.asc.nullslast,criado_em.asc");
    }

    public List<Reclamacao> doCliente(UUID clienteId) {
        return reclamacoes.todos(Map.of("cliente_id", "eq." + clienteId), "criado_em.desc");
    }

    public Reclamacao buscar(UUID id) {
        return reclamacoes.buscar(id).orElseThrow(() -> naoEncontrada(id));
    }

    public Reclamacao abrir(ReclamacaoRequestDTO request, UUID usuario) {
        Map<String, Object> dados = dadosDa(request);
        dados.put("criado_por", usuario);
        dados.put("atualizado_por", usuario);
        Reclamacao aberta = reclamacoes.criar(dados);
        log.info("Reclamação aberta: protocolo={}, por={}", aberta.protocolo(), usuario);
        eventos.publishEvent(new EventoDeNegocio(EventoDeNegocio.RECLAMACAO_ABERTA, aberta));
        return aberta;
    }

    public Reclamacao atualizar(UUID id, ReclamacaoRequestDTO request, UUID usuario) {
        Map<String, Object> dados = dadosDa(request);
        dados.put("atualizado_por", usuario);
        return reclamacoes.atualizar(id, dados).orElseThrow(() -> naoEncontrada(id));
    }

    public Reclamacao alterarStatus(UUID id, StatusReclamacaoRequestDTO request, UUID usuario) {
        Reclamacao atual = buscar(id);
        String solucao = Entradas.opcional(request.solucao());
        if ("RESOLVIDA".equals(request.status()) && solucao == null && atual.solucao() == null) {
            throw new RequisicaoInvalidaException("Descreva a solução para marcar a reclamação como resolvida");
        }
        Map<String, Object> dados = new HashMap<>();
        dados.put("status", request.status());
        dados.put("atualizado_por", usuario);
        if (solucao != null) {
            dados.put("solucao", solucao);
        }
        Reclamacao alterada = reclamacoes.atualizar(id, dados).orElseThrow(() -> naoEncontrada(id));
        String comentario = Entradas.opcional(request.comentario());
        if (comentario != null) {
            comentar(id, comentario, usuario);
        }
        if (!request.status().equals(atual.status())) {
            log.info("Reclamação {}: {} -> {}, por={}", atual.protocolo(), atual.status(), request.status(), usuario);
            eventos.publishEvent(new EventoDeNegocio(EventoDeNegocio.RECLAMACAO_STATUS_ALTERADO, alterada));
        }
        return alterada;
    }

    public ReclamacaoEvento comentar(UUID id, ComentarioRequestDTO request, UUID usuario) {
        buscar(id);
        return comentar(id, request.comentario().strip(), usuario);
    }

    public List<ReclamacaoEvento> historico(UUID id) {
        buscar(id);
        return eventosDaReclamacao.todos(Map.of("reclamacao_id", "eq." + id), "criado_em.asc");
    }

    private ReclamacaoEvento comentar(UUID id, String comentario, UUID usuario) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("reclamacao_id", id);
        dados.put("tipo", "COMENTARIO");
        dados.put("comentario", comentario);
        dados.put("autor_id", usuario);
        return eventosDaReclamacao.criar(dados);
    }

    private static void filtrar(Map<String, String> filtros, String busca, UUID responsavelId) {
        if (responsavelId != null) {
            filtros.put("responsavel_id", "eq." + responsavelId);
        }
        String termo = Entradas.termo(busca);
        if (!termo.isEmpty()) {
            filtros.put("or", Entradas.buscaEm(termo, "protocolo", "assunto", "nome_contato", "contato"));
        }
    }

    private static Map<String, Object> dadosDa(ReclamacaoRequestDTO request) {
        String nomeContato = Entradas.opcional(request.nomeContato());
        if (request.clienteId() == null && nomeContato == null) {
            throw new RequisicaoInvalidaException("Selecione o cliente ou informe o nome de quem reclamou");
        }
        Map<String, Object> dados = new HashMap<>();
        dados.put("cliente_id", request.clienteId());
        dados.put("nome_contato", nomeContato);
        dados.put("contato", Entradas.opcional(request.contato()));
        dados.put("assunto", request.assunto().strip());
        dados.put("descricao", request.descricao().strip());
        dados.put("canal", Entradas.opcional(request.canal()));
        dados.put("prioridade", request.prioridade() == null || request.prioridade().isBlank() ? "MEDIA" : request.prioridade());
        dados.put("responsavel_id", request.responsavelId());
        dados.put("prazo", request.prazo() == null ? null : request.prazo().toString());
        return dados;
    }

    private static RegistroNaoEncontradoException naoEncontrada(UUID id) {
        return new RegistroNaoEncontradoException("Reclamação não encontrada", "Reclamação " + id + " não encontrada");
    }
}
