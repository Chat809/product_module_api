package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.CadastroDAO;
import br.com.triboraizes.produtosapi.dao.CadastroDAO.Consulta;
import br.com.triboraizes.produtosapi.dto.InteracaoRequestDTO;
import br.com.triboraizes.produtosapi.dto.LeadRequestDTO;
import br.com.triboraizes.produtosapi.dto.MoverLeadRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.evento.EventoDeNegocio;
import br.com.triboraizes.produtosapi.exception.OperacaoNaoPermitidaException;
import br.com.triboraizes.produtosapi.exception.RegistroNaoEncontradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.CrmEtapa;
import br.com.triboraizes.produtosapi.model.Interacao;
import br.com.triboraizes.produtosapi.model.Lead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Leads e o funil de vendas: cadastro, arrasto entre etapas e conversão em cliente. */
@Service
public class LeadService {

    private static final Logger log = LoggerFactory.getLogger(LeadService.class);

    private final CadastroDAO<Lead> leads;
    private final ParametrosCrmService parametros;
    private final InteracaoService interacoes;
    private final ApplicationEventPublisher eventos;

    public LeadService(DAOFactory fabrica, ParametrosCrmService parametros, InteracaoService interacoes,
                       ApplicationEventPublisher eventos) {
        this.leads = fabrica.criarLeadDAO();
        this.parametros = parametros;
        this.interacoes = interacoes;
        this.eventos = eventos;
    }

    public PaginaDTO<Lead> listar(String busca, UUID etapaId, UUID origemId, UUID responsavelId, int pagina,
                                  int tamanho) {
        Entradas.validarPagina(pagina, tamanho);
        Consulta consulta = Consulta.de("criado_em.desc,id.asc", pagina, tamanho);
        filtrar(consulta.filtros(), busca, responsavelId);
        if (etapaId != null) {
            consulta.onde("etapa_id", "eq." + etapaId);
        }
        if (origemId != null) {
            consulta.onde("origem_id", "eq." + origemId);
        }
        CadastroDAO.Pagina<Lead> resultado = leads.listar(consulta);
        return PaginaDTO.of(resultado.itens(), l -> l, pagina, tamanho, resultado.total());
    }

    /** Leads do funil (até 1000), na ordem dos cards; o painel agrupa por etapa. */
    public List<Lead> funil(String busca, UUID responsavelId) {
        Map<String, String> filtros = new LinkedHashMap<>();
        filtrar(filtros, busca, responsavelId);
        return leads.todos(filtros, "ordem.asc");
    }

    public Lead buscar(UUID id) {
        return leads.buscar(id).orElseThrow(() -> naoEncontrado(id));
    }

    public Lead criar(LeadRequestDTO request, UUID usuario) {
        Map<String, Object> dados = dadosDo(request);
        dados.put("etapa_id", request.etapaId() != null ? etapaAtiva(request.etapaId()).id() : primeiraEtapa().id());
        dados.put("criado_por", usuario);
        Lead criado = leads.criar(dados);
        log.info("Lead cadastrado: id={}, por={}", criado.id(), usuario);
        eventos.publishEvent(new EventoDeNegocio(EventoDeNegocio.LEAD_CRIADO, criado));
        return criado;
    }

    /** Criação pelo webhook de entrada, sem usuário autenticado. */
    public Lead criarPorIntegracao(LeadRequestDTO request, String descricaoDaOrigem) {
        Map<String, Object> dados = dadosDo(request);
        dados.put("etapa_id", primeiraEtapa().id());
        Lead criado = leads.criar(dados);
        interacoes.anotarNoLead(criado.id(), "Lead recebido pela integração \"" + descricaoDaOrigem + "\".", null);
        log.info("Lead recebido por integração: id={}, origem={}", criado.id(), descricaoDaOrigem);
        eventos.publishEvent(new EventoDeNegocio(EventoDeNegocio.LEAD_CRIADO, criado));
        return criado;
    }

    /** Atualiza os dados do lead; a etapa muda só pelo {@link #mover}. */
    public Lead atualizar(UUID id, LeadRequestDTO request) {
        return leads.atualizar(id, dadosDo(request)).orElseThrow(() -> naoEncontrado(id));
    }

    public Lead mover(UUID id, MoverLeadRequestDTO request, UUID usuario) {
        Lead atual = buscar(id);
        CrmEtapa destino = etapaAtiva(request.etapaId());
        String motivo = Entradas.opcional(request.motivoPerda());
        if ("PERDIDO".equals(destino.tipo()) && motivo == null && atual.motivoPerda() == null) {
            throw new RequisicaoInvalidaException("Informe o motivo da perda");
        }
        Map<String, Object> dados = new HashMap<>();
        dados.put("etapa_id", destino.id());
        dados.put("ordem", request.ordem() != null ? request.ordem() : System.currentTimeMillis() / 1000.0);
        dados.put("motivo_perda", "PERDIDO".equals(destino.tipo()) ? (motivo != null ? motivo : atual.motivoPerda()) : null);
        Lead movido = leads.atualizar(id, dados).orElseThrow(() -> naoEncontrado(id));
        if (!destino.id().equals(atual.etapaId())) {
            String origem = parametros.etapas().stream().filter(e -> e.id().equals(atual.etapaId()))
                    .map(CrmEtapa::nome).findFirst().orElse("?");
            interacoes.anotarNoLead(id, "Etapa alterada de \"" + origem + "\" para \"" + destino.nome() + "\""
                    + (movido.motivoPerda() != null && "PERDIDO".equals(destino.tipo())
                    ? ". Motivo: " + movido.motivoPerda() : "."), usuario);
            eventos.publishEvent(new EventoDeNegocio(EventoDeNegocio.LEAD_ETAPA_ALTERADA, movido));
        }
        return movido;
    }

    /** Cria o cliente com os dados do lead e leva o lead à etapa de ganho (função {@code converter_lead}). */
    public Lead converter(UUID id, UUID usuario) {
        List<Lead> resultado = leads.rpc("converter_lead", Map.<String, Object>of("p_lead_id", id, "p_usuario", usuario));
        Lead convertido = resultado.stream().findFirst().orElseThrow(() -> naoEncontrado(id));
        log.info("Lead convertido em cliente: lead={}, cliente={}, por={}", id, convertido.clienteId(), usuario);
        eventos.publishEvent(new EventoDeNegocio(EventoDeNegocio.LEAD_CONVERTIDO, convertido));
        return convertido;
    }

    public void excluir(UUID id, UUID usuario) {
        if (!leads.excluir(id)) {
            throw naoEncontrado(id);
        }
        log.info("Lead excluído: id={}, por={}", id, usuario);
    }

    public List<Interacao> interacoes(UUID id) {
        buscar(id);
        return interacoes.doLead(id);
    }

    public Interacao registrarInteracao(UUID id, InteracaoRequestDTO request, UUID usuario) {
        buscar(id);
        return interacoes.registrarNoLead(id, request, usuario);
    }

    private CrmEtapa etapaAtiva(UUID id) {
        CrmEtapa etapa = parametros.etapa(id);
        if (!etapa.ativo()) {
            throw new RequisicaoInvalidaException("A etapa \"" + etapa.nome() + "\" está inativa");
        }
        return etapa;
    }

    private CrmEtapa primeiraEtapa() {
        return parametros.etapas().stream().filter(e -> e.ativo() && "ABERTA".equals(e.tipo())).findFirst()
                .orElseThrow(() -> new OperacaoNaoPermitidaException(
                        "Cadastre uma etapa aberta nos parâmetros do CRM antes de criar leads"));
    }

    private static void filtrar(Map<String, String> filtros, String busca, UUID responsavelId) {
        if (responsavelId != null) {
            filtros.put("responsavel_id", "eq." + responsavelId);
        }
        String termo = Entradas.termo(busca);
        if (!termo.isEmpty()) {
            filtros.put("or", Entradas.buscaEm(termo, "nome", "empresa", "email", "telefone"));
        }
    }

    private static Map<String, Object> dadosDo(LeadRequestDTO request) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", request.nome().strip());
        dados.put("empresa", Entradas.opcional(request.empresa()));
        dados.put("email", Entradas.minusculo(request.email()));
        dados.put("telefone", Entradas.opcional(request.telefone()));
        dados.put("origem_id", request.origemId());
        dados.put("valor_estimado", request.valorEstimado());
        dados.put("responsavel_id", request.responsavelId());
        dados.put("proxima_acao", Entradas.opcional(request.proximaAcao()));
        dados.put("proxima_acao_em", request.proximaAcaoEm() == null ? null : request.proximaAcaoEm().toString());
        dados.put("observacoes", Entradas.opcional(request.observacoes()));
        return dados;
    }

    private static RegistroNaoEncontradoException naoEncontrado(UUID id) {
        return new RegistroNaoEncontradoException("Lead não encontrado", "Lead " + id + " não encontrado");
    }
}
