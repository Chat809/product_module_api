package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.client.SupabaseAuthClient;
import br.com.triboraizes.produtosapi.dao.PerfilDAO;
import br.com.triboraizes.produtosapi.dto.AtualizarUsuarioRequestDTO;
import br.com.triboraizes.produtosapi.dto.CriarUsuarioRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioOpcaoDTO;
import br.com.triboraizes.produtosapi.exception.OperacaoNaoPermitidaException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.exception.ServicoDeUsuariosIndisponivelException;
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
    private final AvatarService avatarService;

    public UsuarioService(PerfilDAO perfilDAO, SupabaseAuthClient authClient, AvatarService avatarService) {
        this.perfilDAO = perfilDAO;
        this.authClient = authClient;
        this.avatarService = avatarService;
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
        PerfilDAO.DadosDoUsuario dados = new PerfilDAO.DadosDoUsuario(nome, email, request.papel(), acessos,
                opcional(request.departamento()), opcional(request.funcao()));
        UsuarioDTO atualizado = perfilDAO.atualizar(id, dados)
                .map(UsuarioDTO::from)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        log.info("Usuário atualizado: usuario={}, papel={}, acessos={}, por={}", id, request.papel(), acessos, solicitante);
        return atualizado;
    }

    /**
     * A conta nasce no Supabase Auth com o e-mail já confirmado; o trigger cria o perfil padrão e em seguida
     * ele recebe o nível, os acessos, o departamento e o cargo escolhidos pelo admin.
     */
    public UsuarioDTO criar(CriarUsuarioRequestDTO request, UUID solicitante) {
        List<String> acessos = Acessos.normalizar(request.papel(), request.acessos());
        String nome = request.nome().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        UUID id = authClient.criarUsuario(nome, email, request.senha());
        if (id == null) {
            throw new ServicoDeUsuariosIndisponivelException("O serviço de usuários não devolveu a conta criada", null);
        }
        PerfilDAO.DadosDoUsuario dados = new PerfilDAO.DadosDoUsuario(nome, email, request.papel(), acessos,
                opcional(request.departamento()), opcional(request.funcao()));
        UsuarioDTO criado = perfilDAO.atualizar(id, dados)
                .map(UsuarioDTO::from)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        log.info("Usuário criado pelo admin: usuario={}, papel={}, acessos={}, por={}", id, request.papel(), acessos, solicitante);
        return criado;
    }

    /** O admin não pode excluir a própria conta, o que também garante que sempre reste um administrador. */
    public void excluir(UUID id, UUID solicitante) {
        if (id.equals(solicitante)) {
            throw new OperacaoNaoPermitidaException("Você não pode excluir a sua própria conta");
        }
        if (perfil(id).avatarUrl() != null) {
            avatarService.remover(id);
        }
        authClient.excluirUsuario(id);
        log.info("Usuário excluído: usuario={}, por={}", id, solicitante);
    }

    public void redefinirSenha(UUID id, String senha, UUID solicitante) {
        authClient.redefinirSenha(id, senha);
        log.info("Senha redefinida: usuario={}, por={}", id, solicitante);
    }

    public UsuarioDTO trocarAvatar(UUID id, byte[] conteudo, UUID solicitante) {
        UsuarioDTO atualizado = UsuarioDTO.from(avatarService.trocar(id, conteudo));
        log.info("Avatar definido pelo admin: usuario={}, por={}", id, solicitante);
        return atualizado;
    }

    public UsuarioDTO removerAvatar(UUID id, UUID solicitante) {
        UsuarioDTO atualizado = UsuarioDTO.from(avatarService.remover(id));
        log.info("Avatar removido pelo admin: usuario={}, por={}", id, solicitante);
        return atualizado;
    }

    private Perfil perfil(UUID id) {
        return perfilDAO.buscarPorId(id).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    private static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
