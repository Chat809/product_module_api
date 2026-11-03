package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.PerfilDAO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioDTO;
import br.com.triboraizes.produtosapi.exception.OperacaoNaoPermitidaException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.exception.UsuarioNaoEncontradoException;
import br.com.triboraizes.produtosapi.model.Papel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UsuarioService {

    public static final int TAMANHO_MAXIMO_PAGINA = 100;

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final PerfilDAO perfilDAO;

    public UsuarioService(PerfilDAO perfilDAO) {
        this.perfilDAO = perfilDAO;
    }

    public UsuarioDTO buscar(UUID id) {
        return perfilDAO.buscarPorId(id).map(UsuarioDTO::from).orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    public PaginaDTO<UsuarioDTO> listar(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new RequisicaoInvalidaException(
                    "Use 'page' a partir de 0 e 'size' entre 1 e " + TAMANHO_MAXIMO_PAGINA);
        }
        return PaginaDTO.of(perfilDAO.listar(pagina, tamanho), UsuarioDTO::from, pagina, tamanho, perfilDAO.contar());
    }

    /** Impede que o admin rebaixe a si mesmo, o que garante que sempre reste ao menos um administrador. */
    public UsuarioDTO alterarPapel(UUID id, Papel papel, UUID solicitante) {
        if (id.equals(solicitante) && papel != Papel.ADMIN) {
            throw new OperacaoNaoPermitidaException("Você não pode remover o seu próprio acesso de administrador");
        }
        UsuarioDTO atualizado = perfilDAO.atualizarPapel(id, papel)
                .map(UsuarioDTO::from)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
        log.info("Papel alterado: usuario={}, papel={}, por={}", id, papel, solicitante);
        return atualizado;
    }
}
