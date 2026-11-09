package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.CadastroDAO;
import br.com.triboraizes.produtosapi.dao.CadastroDAO.Consulta;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.PreferenciaNotificacaoDTO;
import br.com.triboraizes.produtosapi.dto.PreferenciasNotificacaoDTO;
import br.com.triboraizes.produtosapi.exception.RegistroNaoEncontradoException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.Notificacao;
import br.com.triboraizes.produtosapi.model.PreferenciaNotificacao;
import br.com.triboraizes.produtosapi.model.TipoNotificacao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Notificações do próprio usuário. Quem cria as notificações é o banco (gatilhos e rotina diária), que também
 * respeita as preferências; aqui ficam a leitura, a marcação como lida e a edição das preferências.
 */
@Service
public class NotificacaoService {

    private final CadastroDAO<Notificacao> notificacoes;
    private final CadastroDAO<PreferenciaNotificacao> preferencias;
    private final boolean emailHabilitado;

    public NotificacaoService(DAOFactory fabrica, @Value("${app.email.habilitado:false}") boolean emailHabilitado) {
        this.notificacoes = fabrica.criarNotificacaoDAO();
        this.preferencias = fabrica.criarPreferenciaNotificacaoDAO();
        this.emailHabilitado = emailHabilitado;
    }

    public PaginaDTO<Notificacao> listar(UUID usuario, boolean somenteNaoLidas, int pagina, int tamanho) {
        Entradas.validarPagina(pagina, tamanho);
        Consulta consulta = Consulta.de("criado_em.desc,id.asc", pagina, tamanho).onde("usuario_id", "eq." + usuario);
        if (somenteNaoLidas) {
            consulta.onde("lida_em", "is.null");
        }
        CadastroDAO.Pagina<Notificacao> resultado = notificacoes.listar(consulta);
        return PaginaDTO.of(resultado.itens(), n -> n, pagina, tamanho, resultado.total());
    }

    public long naoLidas(UUID usuario) {
        return notificacoes.listar(Consulta.de("id.asc", 0, 1)
                .onde("usuario_id", "eq." + usuario)
                .onde("lida_em", "is.null")).total();
    }

    public void marcarLida(UUID id, UUID usuario) {
        int alteradas = notificacoes.atualizarOnde(
                Map.of("id", "eq." + id, "usuario_id", "eq." + usuario, "lida_em", "is.null"),
                Map.<String, Object>of("lida_em", OffsetDateTime.now().toString()));
        if (alteradas == 0 && notificacoes.listar(Consulta.de("id.asc", 0, 1)
                .onde("id", "eq." + id).onde("usuario_id", "eq." + usuario)).total() == 0) {
            throw new RegistroNaoEncontradoException("Notificação não encontrada", "Notificação " + id + " não encontrada");
        }
    }

    public int marcarTodasLidas(UUID usuario) {
        return notificacoes.atualizarOnde(Map.of("usuario_id", "eq." + usuario, "lida_em", "is.null"),
                Map.<String, Object>of("lida_em", OffsetDateTime.now().toString()));
    }

    /** Todos os tipos, com o que o usuário gravou ou o padrão (no painel sim, por e-mail não). */
    public PreferenciasNotificacaoDTO preferencias(UUID usuario) {
        Map<String, PreferenciaNotificacao> gravadas = preferencias
                .todos(Map.of("usuario_id", "eq." + usuario), "tipo.asc").stream()
                .collect(Collectors.toMap(PreferenciaNotificacao::tipo, Function.identity()));
        List<PreferenciaNotificacaoDTO> lista = Arrays.stream(TipoNotificacao.values())
                .map(tipo -> {
                    PreferenciaNotificacao gravada = gravadas.get(tipo.name());
                    return new PreferenciaNotificacaoDTO(tipo, tipo.descricao(),
                            gravada == null || gravada.noApp(), gravada != null && gravada.porEmail());
                })
                .toList();
        return new PreferenciasNotificacaoDTO(emailHabilitado, lista);
    }

    public PreferenciasNotificacaoDTO salvarPreferencias(UUID usuario, List<PreferenciaNotificacaoDTO> novas) {
        List<Map<String, Object>> linhas = novas.stream()
                .map(p -> {
                    Map<String, Object> linha = new HashMap<>();
                    linha.put("usuario_id", usuario);
                    linha.put("tipo", p.tipo().name());
                    linha.put("no_app", p.noApp());
                    linha.put("por_email", p.porEmail());
                    return linha;
                })
                .toList();
        preferencias.gravarVarias(linhas, "usuario_id,tipo");
        return preferencias(usuario);
    }
}
