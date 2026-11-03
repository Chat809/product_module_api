package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.client.SupabaseStorageClient;
import br.com.triboraizes.produtosapi.dao.PerfilDAO;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.exception.UsuarioNaoEncontradoException;
import br.com.triboraizes.produtosapi.model.FormatoDeImagem;
import br.com.triboraizes.produtosapi.model.Perfil;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Avatar de um perfil no bucket público {@code avatars}: usado pelo próprio usuário e pelo admin. */
@Service
public class AvatarService {

    public static final String BUCKET_AVATARES = "avatars";
    public static final int TAMANHO_MAXIMO_AVATAR = 2 * 1024 * 1024;

    private final PerfilDAO perfilDAO;
    private final SupabaseStorageClient storageClient;

    public AvatarService(PerfilDAO perfilDAO, SupabaseStorageClient storageClient) {
        this.perfilDAO = perfilDAO;
        this.storageClient = storageClient;
    }

    /** Cada envio gera um nome novo (UUID), o que evita cache de imagem antiga e não expõe dados do usuário. */
    public Perfil trocar(UUID id, byte[] conteudo) {
        if (conteudo == null || conteudo.length == 0) {
            throw new RequisicaoInvalidaException("Envie uma imagem no campo 'arquivo'");
        }
        if (conteudo.length > TAMANHO_MAXIMO_AVATAR) {
            throw new RequisicaoInvalidaException("A imagem deve ter no máximo 2 MB");
        }
        FormatoDeImagem formato = FormatoDeImagem.detectar(conteudo).orElseThrow(() ->
                new RequisicaoInvalidaException("Formato não suportado. Envie uma imagem PNG, JPEG ou WEBP"));

        String anterior = perfil(id).avatarUrl();
        String url = storageClient.enviar(BUCKET_AVATARES, id.toString(),
                UUID.randomUUID() + "." + formato.extensao(), conteudo, formato.tipo());
        Perfil atualizado = perfilDAO.atualizarAvatar(id, url).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        removerArquivo(anterior);
        return atualizado;
    }

    public Perfil remover(UUID id) {
        String anterior = perfil(id).avatarUrl();
        Perfil atualizado = perfilDAO.atualizarAvatar(id, null).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        removerArquivo(anterior);
        return atualizado;
    }

    private void removerArquivo(String url) {
        storageClient.caminhoDe(BUCKET_AVATARES, url).ifPresent(caminho -> storageClient.remover(BUCKET_AVATARES, caminho));
    }

    private Perfil perfil(UUID id) {
        return perfilDAO.buscarPorId(id).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }
}
