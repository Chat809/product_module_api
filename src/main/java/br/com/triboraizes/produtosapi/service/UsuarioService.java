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
import br.com.triboraizes.produtosapi.model.Acessos;
import br.com.triboraizes.produtosapi.model.Papel;
import br.com.triboraizes.produtosapi.model.Perfil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UsuarioService {

    public static final int TAMANHO_MAXIMO_PAGINA = 100;

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final PerfilDAO perfilDAO;
    private final SupabaseAuthClient authClient;

    public UsuarioService(PerfilDAO perfilDAO, SupabaseAuthClient authClient) {
        this.perfilDAO = perfilDAO;
        this.authClient = authClient;
    }

    public UsuarioDTO buscar(UUID id) {
        return UsuarioDTO.from(perfil(id));
    }

    public PaginaDTO<UsuarioDTO> listar(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new RequisicaoInvalidaException(
                    "Use 'page' a partir de 0 e 'size' entre 1 e " + TAMANHO_MAXIMO_PAGINA);
        }
        return PaginaDTO.of(perfilDAO.listar(pagina, tamanho), UsuarioDTO::from, pagina, tamanho, perfilDAO.contar());
    }

    public List<UsuarioOpcaoDTO> opcoes() {
        return perfilDAO.listarOpcoes().stream().map(UsuarioOpcaoDTO::from).toList();
    }

    /**
     * Nome e e-mail vivem no Supabase Auth (é com o e-mail que se entra), então só são enviados a ele quando mudam.
     * O admin não pode rebaixar a si mesmo, o que garante que sempre reste ao menos um administrador.
     */
    public UsuarioDTO atualizar(UUID id, AtualizarUsuarioRequestDTO request, UUID solicitante) {
        if (id.equals(solicitante) && request.papel() != Papel.ADMIN) {
            throw new OperacaoNaoPermitidaException("Você não pode remover o seu próprio acesso de administrador");
        }
        List<String> acessos = Acessos.normalizar(request.papel(), request.acessos());
        String nome = request.nome().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Perfil atual = perfil(id);
        if (!nome.equals(atual.nome()) || !email.equals(atual.email())) {
            authClient.atualizarDados(id, nome, email);
        }
        UsuarioDTO atualizado = perfilDAO.atualizar(id, nome, email, request.papel(), acessos)
                .map(UsuarioDTO::from)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        log.info("Usuário atualizado: usuario={}, papel={}, acessos={}, por={}", id, request.papel(), acessos, solicitante);
        return atualizado;
    }

    public void redefinirSenha(UUID id, String senha, UUID solicitante) {
        authClient.redefinirSenha(id, senha);
        log.info("Senha redefinida: usuario={}, por={}", id, solicitante);
    }

    private Perfil perfil(UUID id) {
        return perfilDAO.buscarPorId(id).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }
}
