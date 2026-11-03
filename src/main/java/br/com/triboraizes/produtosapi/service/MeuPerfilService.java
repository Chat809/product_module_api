package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.client.SupabaseAuthClient;
import br.com.triboraizes.produtosapi.dao.PerfilDAO;
import br.com.triboraizes.produtosapi.dto.AtualizarMeuPerfilRequestDTO;
import br.com.triboraizes.produtosapi.dto.MeuPerfilDTO;
import br.com.triboraizes.produtosapi.exception.UsuarioNaoEncontradoException;
import br.com.triboraizes.produtosapi.model.Perfil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * "Meu perfil": cada usuário edita nome, telefone, bio e avatar.
 * E-mail, nível de acesso, acessos, departamento e cargo continuam com o admin.
 */
@Service
public class MeuPerfilService {

    private static final Logger log = LoggerFactory.getLogger(MeuPerfilService.class);

    private final PerfilDAO perfilDAO;
    private final SupabaseAuthClient authClient;
    private final AvatarService avatarService;

    public MeuPerfilService(PerfilDAO perfilDAO, SupabaseAuthClient authClient, AvatarService avatarService) {
        this.perfilDAO = perfilDAO;
        this.authClient = authClient;
        this.avatarService = avatarService;
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
        Perfil atualizado = perfilDAO.atualizarDadosProprios(id, nome, opcional(request.telefone()), opcional(request.bio()))
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        log.info("Perfil atualizado pelo próprio usuário: usuario={}", id);
        return MeuPerfilDTO.from(atualizado);
    }

    public MeuPerfilDTO trocarAvatar(UUID id, byte[] conteudo) {
        MeuPerfilDTO atualizado = MeuPerfilDTO.from(avatarService.trocar(id, conteudo));
        log.info("Avatar atualizado: usuario={}", id);
        return atualizado;
    }

    public MeuPerfilDTO removerAvatar(UUID id) {
        MeuPerfilDTO atualizado = MeuPerfilDTO.from(avatarService.remover(id));
        log.info("Avatar removido: usuario={}", id);
        return atualizado;
    }

    private Perfil perfil(UUID id) {
        return perfilDAO.buscarPorId(id).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    private static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
