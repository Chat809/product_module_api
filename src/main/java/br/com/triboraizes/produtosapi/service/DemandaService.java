package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.DemandaDAO;
import br.com.triboraizes.produtosapi.dao.PerfilDAO;
import br.com.triboraizes.produtosapi.dto.DemandaDTO;
import br.com.triboraizes.produtosapi.dto.DemandaRequestDTO;
import br.com.triboraizes.produtosapi.dto.MoverDemandaRequestDTO;
import br.com.triboraizes.produtosapi.exception.DemandaNaoEncontradaException;
import br.com.triboraizes.produtosapi.exception.OperacaoNaoPermitidaException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.model.Demanda;
import br.com.triboraizes.produtosapi.model.FiltroDemandas;
import br.com.triboraizes.produtosapi.model.Perfil;
import br.com.triboraizes.produtosapi.model.StatusDemanda;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kanban de demandas. Não existe um módulo separado de tarefas: as demandas abertas em que a pessoa
 * é responsável são as "Minhas tarefas" do perfil.
 */
@Service
public class DemandaService {

    private static final Logger log = LoggerFactory.getLogger(DemandaService.class);

    private final DemandaDAO demandaDAO;
    private final PerfilDAO perfilDAO;
    private final Clock relogio;

    public DemandaService(DemandaDAO demandaDAO, PerfilDAO perfilDAO, Clock relogio) {
        this.demandaDAO = demandaDAO;
        this.perfilDAO = perfilDAO;
        this.relogio = relogio;
    }

    public List<DemandaDTO> listar(FiltroDemandas filtro) {
        LocalDate hoje = LocalDate.now(relogio);
        return demandaDAO.listar(filtro).stream().map(demanda -> DemandaDTO.from(demanda, hoje)).toList();
    }

    public DemandaDTO buscar(UUID id) {
        return DemandaDTO.from(demanda(id), LocalDate.now(relogio));
    }

    public DemandaDTO criar(DemandaRequestDTO request, UUID solicitante) {
        StatusDemanda status = request.status() == null ? StatusDemanda.A_FAZER : request.status();
        Demanda criada = demandaDAO.criar(paraModelo(request, status, solicitante));
        log.info("Demanda criada: id={}, por={}", criada.id(), solicitante);
        return DemandaDTO.from(criada, LocalDate.now(relogio));
    }

    public DemandaDTO atualizar(UUID id, DemandaRequestDTO request) {
        StatusDemanda status = request.status() == null ? demanda(id).status() : request.status();
        Demanda atualizada = demandaDAO.atualizar(id, paraModelo(request, status, null))
                .orElseThrow(() -> new DemandaNaoEncontradaException(id));
        return DemandaDTO.from(atualizada, LocalDate.now(relogio));
    }

    public DemandaDTO mover(UUID id, MoverDemandaRequestDTO request) {
        Demanda movida = demandaDAO.mover(id, request.status(), request.posicao())
                .orElseThrow(() -> new DemandaNaoEncontradaException(id));
        return DemandaDTO.from(movida, LocalDate.now(relogio));
    }

    /** Qualquer pessoa com o módulo edita e move; excluir é só de quem criou ou de um administrador. */
    public void excluir(UUID id, UUID solicitante, boolean administrador) {
        Demanda demanda = demanda(id);
        if (!administrador && !solicitante.equals(demanda.criadoPor())) {
            throw new OperacaoNaoPermitidaException("Só quem criou a demanda ou um administrador pode excluí-la");
        }
        if (!demandaDAO.excluir(id)) {
            throw new DemandaNaoEncontradaException(id);
        }
        log.info("Demanda excluída: id={}, por={}", id, solicitante);
    }

    private Demanda paraModelo(DemandaRequestDTO request, StatusDemanda status, UUID criadoPor) {
        List<UUID> responsaveis = request.responsaveis() == null ? List.of() : request.responsaveis().stream().distinct().toList();
        validarResponsaveis(responsaveis);
        String descricao = request.descricao() == null || request.descricao().isBlank() ? null : request.descricao().trim();
        return new Demanda(null, request.titulo().trim(), descricao, status, request.urgencia(), request.tipo(),
                request.prazo(), responsaveis, request.checklist() == null ? List.of() : request.checklist(),
                0, criadoPor, null, null);
    }

    private void validarResponsaveis(List<UUID> responsaveis) {
        if (responsaveis.isEmpty()) {
            return;
        }
        Set<UUID> existentes = perfilDAO.listarOpcoes().stream().map(Perfil::id).collect(Collectors.toSet());
        responsaveis.stream().filter(id -> !existentes.contains(id)).findFirst().ifPresent(id -> {
            throw new RequisicaoInvalidaException("Responsável não encontrado: " + id);
        });
    }

    private Demanda demanda(UUID id) {
        return demandaDAO.buscarPorId(id).orElseThrow(() -> new DemandaNaoEncontradaException(id));
    }
}
