package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.CadastroDAO;
import br.com.triboraizes.produtosapi.dto.EtapaRequestDTO;
import br.com.triboraizes.produtosapi.dto.OrigemRequestDTO;
import br.com.triboraizes.produtosapi.exception.RegistroNaoEncontradoException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.CrmEtapa;
import br.com.triboraizes.produtosapi.model.CrmOrigem;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Etapas do funil e origens de leads, editadas em Configurações > Parâmetros do CRM. */
@Service
public class ParametrosCrmService {

    private final CadastroDAO<CrmEtapa> etapas;
    private final CadastroDAO<CrmOrigem> origens;

    public ParametrosCrmService(DAOFactory fabrica) {
        this.etapas = fabrica.criarEtapaDAO();
        this.origens = fabrica.criarOrigemDAO();
    }

    /** Todas as etapas, ativas e inativas, na ordem do funil. */
    public List<CrmEtapa> etapas() {
        return etapas.todos(Map.of(), "ordem.asc,nome.asc");
    }

    public CrmEtapa etapa(UUID id) {
        return etapas.buscar(id).orElseThrow(() -> new RegistroNaoEncontradoException("Etapa não encontrada",
                "Etapa " + id + " não encontrada"));
    }

    public CrmEtapa criarEtapa(EtapaRequestDTO request) {
        return etapas.criar(dadosDa(request));
    }

    public CrmEtapa atualizarEtapa(UUID id, EtapaRequestDTO request) {
        return etapas.atualizar(id, dadosDa(request)).orElseThrow(() -> new RegistroNaoEncontradoException(
                "Etapa não encontrada", "Etapa " + id + " não encontrada"));
    }

    public void excluirEtapa(UUID id) {
        if (!etapas.excluir(id)) {
            throw new RegistroNaoEncontradoException("Etapa não encontrada", "Etapa " + id + " não encontrada");
        }
    }

    public List<CrmOrigem> origens() {
        return origens.todos(Map.of(), "nome.asc");
    }

    public CrmOrigem criarOrigem(OrigemRequestDTO request) {
        return origens.criar(dadosDa(request));
    }

    public CrmOrigem atualizarOrigem(UUID id, OrigemRequestDTO request) {
        return origens.atualizar(id, dadosDa(request)).orElseThrow(() -> new RegistroNaoEncontradoException(
                "Origem não encontrada", "Origem " + id + " não encontrada"));
    }

    public void excluirOrigem(UUID id) {
        if (!origens.excluir(id)) {
            throw new RegistroNaoEncontradoException("Origem não encontrada", "Origem " + id + " não encontrada");
        }
    }

    private static Map<String, Object> dadosDa(EtapaRequestDTO request) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", request.nome().strip());
        dados.put("ordem", request.ordem() == null ? 0 : request.ordem());
        dados.put("cor", request.cor() == null || request.cor().isBlank() ? "#64748b" : request.cor().toLowerCase());
        dados.put("tipo", request.tipo() == null || request.tipo().isBlank() ? "ABERTA" : request.tipo());
        dados.put("ativo", request.ativo() == null || request.ativo());
        return dados;
    }

    private static Map<String, Object> dadosDa(OrigemRequestDTO request) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", request.nome().strip());
        dados.put("ativo", request.ativo() == null || request.ativo());
        return dados;
    }
}
