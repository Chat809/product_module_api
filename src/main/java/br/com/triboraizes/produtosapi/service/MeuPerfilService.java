package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.client.SupabaseAuthClient;
import br.com.triboraizes.produtosapi.client.SupabaseStorageClient;
import br.com.triboraizes.produtosapi.dao.PerfilDAO;
import br.com.triboraizes.produtosapi.dto.AtualizarMeuPerfilRequestDTO;
import br.com.triboraizes.produtosapi.dto.MeuPerfilDTO;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.exception.UsuarioNaoEncontradoException;
import br.com.triboraizes.produtosapi.model.FormatoDeImagem;
import br.com.triboraizes.produtosapi.model.Perfil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** "Meu perfil": cada usuário edita os próprios dados e o avatar. E-mail, papel e acessos continuam com o admin. */
@Service
public class MeuPerfilService {

    public static final String BUCKET_AVATARES = "avatars";
    public static final int TAMANHO_MAXIMO_AVATAR = 2 * 1024 * 1024;

    private static final Logger log = LoggerFactory.getLogger(MeuPerfilService.class);

    private final PerfilDAO perfilDAO;
    private final SupabaseAuthClient authClient;
    private final SupabaseStorageClient storageClient;

    public MeuPerfilService(PerfilDAO perfilDAO, SupabaseAuthClient authClient, SupabaseStorageClient storageClient) {
        this.perfilDAO = perfilDAO;
        this.authClient = authClient;
        this.storageClient = storageClient;
    }

    public MeuPerfilDTO buscar(UUID id) {
        return MeuPerfilDTO.from(perfil(id));
    }

    /** O nome também vive no Supabase Auth (vai no token), então só é enviado a ele quando muda. */
    public MeuPerfilDTO atualizar(UUID id, AtualizarMeuPerfilRequestDTO request) {
        String nome = request.nome().trim();
        if (!nome.equals(perfil(id).nome())) {
            authClient.atualizarNome(id, nome);
        }
        Perfil atualizado = perfilDAO.atualizarDadosProprios(id, nome,
                        opcional(request.telefone()), opcional(request.funcao()), opcional(request.bio()))
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        log.info("Perfil atualizado pelo próprio usuário: usuario={}", id);
        return MeuPerfilDTO.from(atualizado);
    }

    /** Cada envio gera um nome novo (UUID), o que evita cache de imagem antiga e não expõe dados do usuário. */
    public MeuPerfilDTO trocarAvatar(UUID id, byte[] conteudo) {
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
        log.info("Avatar atualizado: usuario={}", id);
        return MeuPerfilDTO.from(atualizado);
    }

    public MeuPerfilDTO removerAvatar(UUID id) {
        String anterior = perfil(id).avatarUrl();
        Perfil atualizado = perfilDAO.atualizarAvatar(id, null).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        removerArquivo(anterior);
        log.info("Avatar removido: usuario={}", id);
        return MeuPerfilDTO.from(atualizado);
    }

    private void removerArquivo(String url) {
        storageClient.caminhoDe(BUCKET_AVATARES, url).ifPresent(caminho -> storageClient.remover(BUCKET_AVATARES, caminho));
    }

    private Perfil perfil(UUID id) {
        return perfilDAO.buscarPorId(id).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    private static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
