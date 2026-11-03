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
import br.com.triboraizes.produtosapi.model.ItemChecklist;
import br.com.triboraizes.produtosapi.model.Perfil;
import br.com.triboraizes.produtosapi.model.StatusDemanda;
import br.com.triboraizes.produtosapi.model.TipoDemanda;
import br.com.triboraizes.produtosapi.model.UrgenciaDemanda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class DemandaServiceTest {

    private static final UUID ID = UUID.fromString("7a2d3c1b-5e4f-4a6b-9c8d-1e2f3a4b5c6d");
    private static final UUID MARIA = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID JOAO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final LocalDate HOJE = LocalDate.parse("2026-11-03");

    @Mock
    private DemandaDAO demandaDAO;

    @Mock
    private PerfilDAO perfilDAO;

    private DemandaService service;

    @BeforeEach
    void configurar() {
        Clock relogio = Clock.fixed(Instant.parse("2026-11-03T15:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        service = new DemandaService(demandaDAO, perfilDAO, relogio);
    }

    private static Demanda demanda(StatusDemanda status, LocalDate prazo, UUID criadoPor) {
        return new Demanda(ID, "Relatório", null, status, UrgenciaDemanda.ALTA, TipoDemanda.MELHORIA, prazo,
                List.of(MARIA), List.of(), 0, criadoPor, null, null);
    }

    private static DemandaRequestDTO pedido(StatusDemanda status, List<UUID> responsaveis) {
        return new DemandaRequestDTO(" Relatório ", "  ", TipoDemanda.MELHORIA, UrgenciaDemanda.ALTA, status,
                null, responsaveis, List.of(new ItemChecklist("Revisar", false)));
    }

    private void comUsuarios(UUID... ids) {
        when(perfilDAO.listarOpcoes()).thenReturn(java.util.Arrays.stream(ids)
                .map(id -> new Perfil(id, "Nome", null, null, null, null)).toList());
    }

    @Test
    @DisplayName("Prazo vencido só conta como atrasado enquanto a demanda não for concluída")
    void deveMarcarAtrasadas() {
        FiltroDemandas filtro = new FiltroDemandas(MARIA, null, null, true);
        when(demandaDAO.listar(filtro)).thenReturn(List.of(
                demanda(StatusDemanda.EM_ANDAMENTO, HOJE.minusDays(1), MARIA),
                demanda(StatusDemanda.CONCLUIDO, HOJE.minusDays(1), MARIA),
                demanda(StatusDemanda.A_FAZER, HOJE, MARIA),
                demanda(StatusDemanda.A_FAZER, null, MARIA)));

        assertThat(service.listar(filtro)).extracting(DemandaDTO::atrasada).containsExactly(true, false, false, false);
    }

    @Test
    @DisplayName("Criar deve começar em A_FAZER, gravar o autor, limpar textos e remover responsáveis repetidos")
    void deveCriar() {
        comUsuarios(MARIA, JOAO);
        when(demandaDAO.criar(any())).thenReturn(demanda(StatusDemanda.A_FAZER, null, JOAO));

        service.criar(pedido(null, List.of(MARIA, MARIA)), JOAO);

        ArgumentCaptor<Demanda> gravada = ArgumentCaptor.forClass(Demanda.class);
        verify(demandaDAO).criar(gravada.capture());
        assertThat(gravada.getValue().status()).isEqualTo(StatusDemanda.A_FAZER);
        assertThat(gravada.getValue().criadoPor()).isEqualTo(JOAO);
        assertThat(gravada.getValue().titulo()).isEqualTo("Relatório");
        assertThat(gravada.getValue().descricao()).isNull();
        assertThat(gravada.getValue().responsaveis()).containsExactly(MARIA);
    }

    @Test
    @DisplayName("Responsável inexistente é recusado antes de gravar")
    void deveRecusarResponsavelInexistente() {
        comUsuarios(MARIA);

        assertThatThrownBy(() -> service.criar(pedido(null, List.of(JOAO)), MARIA))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessageContaining(JOAO.toString());
        verify(demandaDAO, never()).criar(any());
    }

    @Test
    @DisplayName("Sem responsáveis não consulta usuários; editar sem status mantém a coluna atual")
    void deveAtualizarMantendoStatus() {
        when(demandaDAO.buscarPorId(ID)).thenReturn(Optional.of(demanda(StatusDemanda.EM_REVISAO, null, MARIA)));
        when(demandaDAO.atualizar(eq(ID), any())).thenReturn(Optional.of(demanda(StatusDemanda.EM_REVISAO, null, MARIA)));

        service.atualizar(ID, pedido(null, null));

        ArgumentCaptor<Demanda> gravada = ArgumentCaptor.forClass(Demanda.class);
        verify(demandaDAO).atualizar(eq(ID), gravada.capture());
        assertThat(gravada.getValue().status()).isEqualTo(StatusDemanda.EM_REVISAO);
        assertThat(gravada.getValue().responsaveis()).isEmpty();
        verifyNoInteractions(perfilDAO);
    }

    @Test
    @DisplayName("Editar, mover ou buscar um id inexistente responde 404")
    void deveLancar404() {
        when(demandaDAO.atualizar(eq(ID), any())).thenReturn(Optional.empty());
        when(demandaDAO.mover(ID, StatusDemanda.CONCLUIDO, 0)).thenReturn(Optional.empty());
        when(demandaDAO.buscarPorId(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(ID, pedido(StatusDemanda.A_FAZER, List.of())))
                .isInstanceOf(DemandaNaoEncontradaException.class);
        assertThatThrownBy(() -> service.mover(ID, new MoverDemandaRequestDTO(StatusDemanda.CONCLUIDO, 0)))
                .isInstanceOf(DemandaNaoEncontradaException.class);
        assertThatThrownBy(() -> service.buscar(ID)).isInstanceOf(DemandaNaoEncontradaException.class);
    }

    @Test
    @DisplayName("Mover deve repassar coluna e posição")
    void deveMover() {
        when(demandaDAO.mover(ID, StatusDemanda.CONCLUIDO, 1))
                .thenReturn(Optional.of(demanda(StatusDemanda.CONCLUIDO, HOJE.minusDays(5), MARIA)));

        DemandaDTO movida = service.mover(ID, new MoverDemandaRequestDTO(StatusDemanda.CONCLUIDO, 1));

        assertThat(movida.status()).isEqualTo(StatusDemanda.CONCLUIDO);
        assertThat(movida.atrasada()).isFalse();
    }

    @Test
    @DisplayName("Só o autor ou um admin excluem")
    void deveControlarExclusao() {
        when(demandaDAO.buscarPorId(ID)).thenReturn(Optional.of(demanda(StatusDemanda.A_FAZER, null, MARIA)));
        when(demandaDAO.excluir(ID)).thenReturn(true);

        assertThatThrownBy(() -> service.excluir(ID, JOAO, false)).isInstanceOf(OperacaoNaoPermitidaException.class);
        service.excluir(ID, MARIA, false);
        service.excluir(ID, JOAO, true);

        verify(demandaDAO, org.mockito.Mockito.times(2)).excluir(ID);
    }

    @Test
    @DisplayName("Excluir uma demanda que sumiu no meio do caminho responde 404")
    void deveLancarSeSumirNaExclusao() {
        when(demandaDAO.buscarPorId(ID)).thenReturn(Optional.of(demanda(StatusDemanda.A_FAZER, null, null)));
        when(demandaDAO.excluir(ID)).thenReturn(false);

        assertThatThrownBy(() -> service.excluir(ID, JOAO, true)).isInstanceOf(DemandaNaoEncontradaException.class);
    }
}
