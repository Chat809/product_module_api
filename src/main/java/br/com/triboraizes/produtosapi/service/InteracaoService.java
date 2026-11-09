package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.CadastroDAO;
import br.com.triboraizes.produtosapi.dto.InteracaoRequestDTO;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.Interacao;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Linha do tempo de contatos com leads e clientes. Quem chama confere antes se o lead ou cliente existe. */
@Service
public class InteracaoService {

    private final CadastroDAO<Interacao> interacoes;

    public InteracaoService(DAOFactory fabrica) {
        this.interacoes = fabrica.criarInteracaoDAO();
    }

    public List<Interacao> doLead(UUID leadId) {
        return interacoes.todos(Map.of("lead_id", "eq." + leadId), "criado_em.desc");
    }

    public List<Interacao> doCliente(UUID clienteId) {
        return interacoes.todos(Map.of("cliente_id", "eq." + clienteId), "criado_em.desc");
    }

    public Interacao registrarNoLead(UUID leadId, InteracaoRequestDTO request, UUID usuario) {
        return registrar("lead_id", leadId, request.tipo(), request.descricao().strip(), usuario);
    }

    public Interacao registrarNoCliente(UUID clienteId, InteracaoRequestDTO request, UUID usuario) {
        return registrar("cliente_id", clienteId, request.tipo(), request.descricao().strip(), usuario);
    }

    /** Nota automática, como a mudança de etapa de um lead. */
    void anotarNoLead(UUID leadId, String descricao, UUID usuario) {
        registrar("lead_id", leadId, "NOTA", descricao, usuario);
    }

    private Interacao registrar(String coluna, UUID id, String tipo, String descricao, UUID usuario) {
        Map<String, Object> dados = new HashMap<>();
        dados.put(coluna, id);
        dados.put("tipo", tipo);
        dados.put("descricao", descricao);
        dados.put("registrado_por", usuario);
        return interacoes.criar(dados);
    }
}
