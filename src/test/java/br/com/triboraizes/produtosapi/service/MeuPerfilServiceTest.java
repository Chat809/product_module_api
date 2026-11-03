package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.client.SupabaseAuthClient;
import br.com.triboraizes.produtosapi.dao.PerfilDAO;
import br.com.triboraizes.produtosapi.dto.AtualizarMeuPerfilRequestDTO;
import br.com.triboraizes.produtosapi.dto.MeuPerfilDTO;
import br.com.triboraizes.produtosapi.exception.UsuarioNaoEncontradoException;
import br.com.triboraizes.produtosapi.model.Papel;
import br.com.triboraizes.produtosapi.model.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class MeuPerfilServiceTest {

    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0};

    @Mock
    private PerfilDAO perfilDAO;

    @Mock
    private SupabaseAuthClient authClient;

    @Mock
    private AvatarService avatarService;

    @InjectMocks
    private MeuPerfilService service;

    private static Perfil perfil(String nome, String avatar) {
        return new Perfil(ID, nome, "maria@exemplo.com", Papel.USUARIO, List.of("dashboard", "demandas"),
                OffsetDateTime.parse("2026-11-03T08:00:00Z"), "(11) 9999-0000", "Compradora", "Bio", avatar, "Compras");
    }

    @Test
    @DisplayName("Deve devolver o perfil completo com acessos, cargo e departamento")
    void deveBuscar() {
        when(perfilDAO.buscarPorId(ID)).thenReturn(Optional.of(perfil("Maria", null)));

        MeuPerfilDTO dto = service.buscar(ID);

        assertThat(dto.funcao()).isEqualTo("Compradora");
        assertThat(dto.departamento()).isEqualTo("Compras");
        assertThat(dto.acessos()).containsExactly("dashboard", "demandas");
    }

    @Test
    @DisplayName("Deve gravar campos vazios como null sem chamar o Auth quando o nome não muda")
    void deveAtualizarSemTrocarNome() {
        when(perfilDAO.buscarPorId(ID)).thenReturn(Optional.of(perfil("Maria", null)));
        when(perfilDAO.atualizarDadosProprios(ID, "Maria", null, null))
                .thenReturn(Optional.of(perfil("Maria", null)));

        service.atualizar(ID, new AtualizarMeuPerfilRequestDTO(" Maria ", "  ", ""));

        verifyNoInteractions(authClient);
    }

    @Test
    @DisplayName("Deve enviar o novo nome ao Auth")
    void deveAtualizarNomeNoAuth() {
        when(perfilDAO.buscarPorId(ID)).thenReturn(Optional.of(perfil("Maria", null)));
        when(perfilDAO.atualizarDadosProprios(ID, "Maria Souza", "(11) 9999-0000", "Bio"))
                .thenReturn(Optional.of(perfil("Maria Souza", null)));

        assertThat(service.atualizar(ID, new AtualizarMeuPerfilRequestDTO("Maria Souza", " (11) 9999-0000 ", "Bio")).nome())
                .isEqualTo("Maria Souza");
        verify(authClient).atualizarNome(ID, "Maria Souza");
    }

    @Test
    @DisplayName("Deve lançar 404 se o perfil não existir")
    void deveLancarSemPerfil() {
        when(perfilDAO.buscarPorId(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(ID)).isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve trocar e remover o próprio avatar pelo serviço de avatar")
    void deveTrocarERemoverAvatar() {
        when(avatarService.trocar(ID, PNG)).thenReturn(perfil("Maria", "https://x/novo.png"));
        when(avatarService.remover(ID)).thenReturn(perfil("Maria", null));

        assertThat(service.trocarAvatar(ID, PNG).avatarUrl()).isEqualTo("https://x/novo.png");
        assertThat(service.removerAvatar(ID).avatarUrl()).isNull();
    }
}
