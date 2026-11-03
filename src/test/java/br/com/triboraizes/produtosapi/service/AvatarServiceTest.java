package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.client.SupabaseStorageClient;
import br.com.triboraizes.produtosapi.dao.PerfilDAO;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String URL_ANTIGA = "https://projeto.supabase.co/storage/v1/object/public/avatars/" + ID + "/velho.png";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0};

    @Mock
    private PerfilDAO perfilDAO;

    @Mock
    private SupabaseStorageClient storageClient;

    @InjectMocks
    private AvatarService service;

    private static Perfil perfil(String avatar) {
        return new Perfil(ID, "Maria", "maria@exemplo.com", Papel.USUARIO, List.of("*"), null,
                null, null, null, avatar, null);
    }

    @Test
    @DisplayName("Deve enviar o avatar com nome aleatório na pasta do usuário e apagar o anterior")
    void deveTrocarAvatar() {
        String nova = "https://projeto.supabase.co/storage/v1/object/public/avatars/" + ID + "/novo.png";
        when(perfilDAO.buscarPorId(ID)).thenReturn(Optional.of(perfil(URL_ANTIGA)));
        when(storageClient.enviar(eq("avatars"), eq(ID.toString()), matches("[0-9a-f-]{36}\\.png"), eq(PNG), eq("image/png")))
                .thenReturn(nova);
        when(perfilDAO.atualizarAvatar(ID, nova)).thenReturn(Optional.of(perfil(nova)));
        when(storageClient.caminhoDe("avatars", URL_ANTIGA)).thenReturn(Optional.of(ID + "/velho.png"));

        assertThat(service.trocar(ID, PNG).avatarUrl()).isEqualTo(nova);
        verify(storageClient).remover("avatars", ID + "/velho.png");
    }

    @Test
    @DisplayName("Deve recusar arquivo vazio, grande demais ou que não seja imagem antes de enviar")
    void deveValidarAvatar() {
        assertThatThrownBy(() -> service.trocar(ID, new byte[0])).isInstanceOf(RequisicaoInvalidaException.class);
        assertThatThrownBy(() -> service.trocar(ID, new byte[AvatarService.TAMANHO_MAXIMO_AVATAR + 1]))
                .hasMessageContaining("2 MB");
        assertThatThrownBy(() -> service.trocar(ID, "<svg/>".getBytes()))
                .hasMessageContaining("PNG, JPEG ou WEBP");
        verifyNoInteractions(storageClient, perfilDAO);
    }

    @Test
    @DisplayName("Deve remover o avatar e apagar o arquivo; URL de fora do bucket não é apagada")
    void deveRemoverAvatar() {
        when(perfilDAO.buscarPorId(ID)).thenReturn(Optional.of(perfil("https://outro.site/a.png")));
        when(perfilDAO.atualizarAvatar(ID, null)).thenReturn(Optional.of(perfil(null)));
        when(storageClient.caminhoDe("avatars", "https://outro.site/a.png")).thenReturn(Optional.empty());

        assertThat(service.remover(ID).avatarUrl()).isNull();
        verify(storageClient, never()).remover(anyString(), any());
    }

    @Test
    @DisplayName("Deve lançar 404 para usuário inexistente sem enviar nada")
    void deveLancarSemPerfil() {
        when(perfilDAO.buscarPorId(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.trocar(ID, PNG)).isInstanceOf(UsuarioNaoEncontradoException.class);
        verifyNoInteractions(storageClient);
    }
}
