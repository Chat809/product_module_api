package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.client.SupabaseAuthClient;
import br.com.triboraizes.produtosapi.dao.PerfilDAO;
import br.com.triboraizes.produtosapi.dto.AtualizarUsuarioRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioOpcaoDTO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final UUID ADMIN = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OUTRO = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final OffsetDateTime CRIADO = OffsetDateTime.parse("2026-11-03T08:00:00Z");

    @Mock
    private PerfilDAO perfilDAO;

    @Mock
    private SupabaseAuthClient authClient;

    @Mock
    private AvatarService avatarService;

    @InjectMocks
    private UsuarioService usuarioService;

    private static Perfil perfil(UUID id, Papel papel) {
        return perfil(id, papel, List.of("dashboard", "produtos"));
    }

    private static Perfil perfil(UUID id, Papel papel, List<String> acessos) {
        return new Perfil(id, "Maria", "maria@exemplo.com", papel, acessos, CRIADO);
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
    @DisplayName("Deve limitar os acessos sem chamar o Auth quando nome e e-mail não mudam")
    void deveAlterarAcessos() {
        when(perfilDAO.buscarPorId(OUTRO)).thenReturn(Optional.of(perfil(OUTRO, Papel.USUARIO)));
        when(perfilDAO.atualizar(OUTRO, dados("Maria", "maria@exemplo.com", Papel.USUARIO, List.of("dashboard"))))
                .thenReturn(Optional.of(perfil(OUTRO, Papel.USUARIO, List.of("dashboard"))));

        UsuarioDTO usuario = usuarioService.atualizar(OUTRO,
                pedido("Maria", "maria@exemplo.com", Papel.USUARIO, List.of("dashboard", "dashboard")), ADMIN);

        assertThat(usuario.acessos()).containsExactly("dashboard");
        verifyNoInteractions(authClient);
    }

    @Test
    @DisplayName("Deve normalizar e enviar ao Auth o novo nome e e-mail")
    void deveAlterarNomeEEmail() {
        when(perfilDAO.buscarPorId(OUTRO)).thenReturn(Optional.of(perfil(OUTRO, Papel.USUARIO)));
        when(perfilDAO.atualizar(OUTRO, dados("Maria Souza", "nova@exemplo.com", Papel.USUARIO, List.of("*"))))
                .thenReturn(Optional.of(perfil(OUTRO, Papel.USUARIO, List.of("*"))));

        usuarioService.atualizar(OUTRO,
                pedido(" Maria Souza ", " Nova@Exemplo.com ", Papel.USUARIO, List.of("*")), ADMIN);

        verify(authClient).atualizarDados(OUTRO, "Maria Souza", "nova@exemplo.com");
    }

    @Test
    @DisplayName("Promover a ADMIN grava acesso total")
    void devePromoverComAcessoTotal() {
        when(perfilDAO.buscarPorId(OUTRO)).thenReturn(Optional.of(perfil(OUTRO, Papel.USUARIO)));
        when(perfilDAO.atualizar(OUTRO, dados("Maria", "maria@exemplo.com", Papel.ADMIN, List.of("*"))))
                .thenReturn(Optional.of(perfil(OUTRO, Papel.ADMIN, List.of("*"))));

        UsuarioDTO usuario = usuarioService.atualizar(OUTRO,
                pedido("Maria", "maria@exemplo.com", Papel.ADMIN, List.of("dashboard")), ADMIN);

        assertThat(usuario.papel()).isEqualTo(Papel.ADMIN);
        assertThat(usuario.acessos()).containsExactly("*");
    }

    @Test
    @DisplayName("Admin não pode rebaixar a si mesmo")
    void naoDeveRebaixarASiMesmo() {
        assertThatThrownBy(() -> usuarioService.atualizar(ADMIN,
                pedido("Maria", "maria@exemplo.com", Papel.USUARIO, List.of("*")), ADMIN))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
        verifyNoInteractions(perfilDAO, authClient);
    }

    @Test
    @DisplayName("Acesso inválido é recusado antes de tocar no banco")
    void deveRecusarAcessoInvalido() {
        assertThatThrownBy(() -> usuarioService.atualizar(OUTRO,
                pedido("Maria", "maria@exemplo.com", Papel.USUARIO, List.of("usuarios")), ADMIN))
                .isInstanceOf(RequisicaoInvalidaException.class);
        verifyNoInteractions(perfilDAO, authClient);
    }

    @Test
    @DisplayName("Deve lançar 404 ao editar um id inexistente")
    void deveLancarAoEditarInexistente() {
        when(perfilDAO.buscarPorId(OUTRO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.atualizar(OUTRO,
                pedido("Maria", "maria@exemplo.com", Papel.USUARIO, List.of("*")), ADMIN))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve lançar 404 se o perfil sumir entre a leitura e a gravação")
    void deveLancarSeSumirNaGravacao() {
        when(perfilDAO.buscarPorId(OUTRO)).thenReturn(Optional.of(perfil(OUTRO, Papel.USUARIO)));
        when(perfilDAO.atualizar(OUTRO, dados("Maria", "maria@exemplo.com", Papel.USUARIO, List.of("*"))))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.atualizar(OUTRO,
                pedido("Maria", "maria@exemplo.com", Papel.USUARIO, List.of("*")), ADMIN))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Opções de responsável expõem só id, nome e avatar")
    void deveListarOpcoes() {
        when(perfilDAO.listarOpcoes()).thenReturn(List.of(
                new Perfil(OUTRO, "Maria", null, null, null, null, null, null, null, "https://x/a.png", null)));

        assertThat(usuarioService.opcoes()).containsExactly(new UsuarioOpcaoDTO(OUTRO, "Maria", "https://x/a.png"));
    }

    @Test
    @DisplayName("Deve repassar a nova senha ao Auth")
    void deveRedefinirSenha() {
        usuarioService.redefinirSenha(OUTRO, "novaSenha1", ADMIN);

        verify(authClient).redefinirSenha(OUTRO, "novaSenha1");
    }

    @Test
    @DisplayName("Deve gravar departamento e cargo aparados; vazios viram null")
    void deveGravarDepartamentoECargo() {
        when(perfilDAO.buscarPorId(OUTRO)).thenReturn(Optional.of(perfil(OUTRO, Papel.USUARIO)));
        PerfilDAO.DadosDoUsuario esperado = new PerfilDAO.DadosDoUsuario(
                "Maria", "maria@exemplo.com", Papel.USUARIO, List.of("*"), "Tecnologia", null);
        when(perfilDAO.atualizar(OUTRO, esperado)).thenReturn(Optional.of(perfil(OUTRO, Papel.USUARIO)));

        usuarioService.atualizar(OUTRO, new AtualizarUsuarioRequestDTO(
                "Maria", "maria@exemplo.com", Papel.USUARIO, List.of("*"), " Tecnologia ", "  "), ADMIN);

        verify(perfilDAO).atualizar(OUTRO, esperado);
    }

    @Test
    @DisplayName("Deve trocar e remover a foto de outro usuário pelo serviço de avatar")
    void deveTrocarERemoverAvatar() {
        byte[] png = {1, 2, 3};
        Perfil comFoto = new Perfil(OUTRO, "Maria", "maria@exemplo.com", Papel.USUARIO, List.of("*"), CRIADO,
                null, "Desenvolvedora", null, "https://x/a.png", "Tecnologia");
        when(avatarService.trocar(OUTRO, png)).thenReturn(comFoto);
        when(avatarService.remover(OUTRO)).thenReturn(perfil(OUTRO, Papel.USUARIO));

        UsuarioDTO comAvatar = usuarioService.trocarAvatar(OUTRO, png, ADMIN);

        assertThat(comAvatar.avatarUrl()).isEqualTo("https://x/a.png");
        assertThat(comAvatar.departamento()).isEqualTo("Tecnologia");
        assertThat(comAvatar.funcao()).isEqualTo("Desenvolvedora");
        assertThat(usuarioService.removerAvatar(OUTRO, ADMIN).avatarUrl()).isNull();
    }

    private static AtualizarUsuarioRequestDTO pedido(String nome, String email, Papel papel, List<String> acessos) {
        return new AtualizarUsuarioRequestDTO(nome, email, papel, acessos, null, null);
    }

    private static PerfilDAO.DadosDoUsuario dados(String nome, String email, Papel papel, List<String> acessos) {
        return new PerfilDAO.DadosDoUsuario(nome, email, papel, acessos, null, null);
    }
}
