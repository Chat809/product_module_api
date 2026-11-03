package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.PerfilDAO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioDTO;
import br.com.triboraizes.produtosapi.exception.OperacaoNaoPermitidaException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.exception.UsuarioNaoEncontradoException;
import br.com.triboraizes.produtosapi.model.Papel;
import br.com.triboraizes.produtosapi.model.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final UUID ADMIN = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTRO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final OffsetDateTime CRIADO = OffsetDateTime.parse("2026-11-03T08:00:00Z");

    @Mock
    private PerfilDAO perfilDAO;

    @InjectMocks
    private UsuarioService usuarioService;

    private static Perfil perfil(UUID id, Papel papel) {
        return new Perfil(id, "Maria", "maria@exemplo.com", papel, CRIADO);
    }

    @Test
    @DisplayName("Deve buscar o usuário e converter a data de criação")
    void deveBuscar() {
        when(perfilDAO.buscarPorId(OUTRO)).thenReturn(Optional.of(perfil(OUTRO, Papel.USUARIO)));

        UsuarioDTO usuario = usuarioService.buscar(OUTRO);

        assertThat(usuario.papel()).isEqualTo(Papel.USUARIO);
        assertThat(usuario.criadoEm()).isEqualTo(Instant.parse("2026-11-03T08:00:00Z"));
    }

    @Test
    @DisplayName("Deve lançar 404 quando o perfil não existir")
    void deveLancarQuandoNaoExistir() {
        when(perfilDAO.buscarPorId(OUTRO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscar(OUTRO)).isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve listar a página com o total de usuários")
    void deveListar() {
        when(perfilDAO.listar(0, 20)).thenReturn(List.of(perfil(ADMIN, Papel.ADMIN), perfil(OUTRO, Papel.USUARIO)));
        when(perfilDAO.contar()).thenReturn(2L);

        PaginaDTO<UsuarioDTO> pagina = usuarioService.listar(0, 20);

        assertThat(pagina.conteudo()).hasSize(2);
        assertThat(pagina.totalElementos()).isEqualTo(2);
        assertThat(pagina.totalPaginas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve recusar paginação fora dos limites")
    void deveRecusarPaginacaoInvalida() {
        assertThatThrownBy(() -> usuarioService.listar(-1, 20)).isInstanceOf(RequisicaoInvalidaException.class);
        assertThatThrownBy(() -> usuarioService.listar(0, 0)).isInstanceOf(RequisicaoInvalidaException.class);
        assertThatThrownBy(() -> usuarioService.listar(0, 101)).isInstanceOf(RequisicaoInvalidaException.class);
        verifyNoInteractions(perfilDAO);
    }

    @Test
    @DisplayName("Admin deve conseguir promover outro usuário")
    void devePromover() {
        when(perfilDAO.atualizarPapel(OUTRO, Papel.ADMIN)).thenReturn(Optional.of(perfil(OUTRO, Papel.ADMIN)));

        assertThat(usuarioService.alterarPapel(OUTRO, Papel.ADMIN, ADMIN).papel()).isEqualTo(Papel.ADMIN);
    }

    @Test
    @DisplayName("Admin não pode rebaixar a si mesmo")
    void naoDeveRebaixarASiMesmo() {
        assertThatThrownBy(() -> usuarioService.alterarPapel(ADMIN, Papel.USUARIO, ADMIN))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
        verifyNoInteractions(perfilDAO);
    }

    @Test
    @DisplayName("Deve lançar 404 ao alterar o papel de um id inexistente")
    void deveLancarAoAlterarInexistente() {
        when(perfilDAO.atualizarPapel(OUTRO, Papel.USUARIO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.alterarPapel(OUTRO, Papel.USUARIO, ADMIN))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }
}
