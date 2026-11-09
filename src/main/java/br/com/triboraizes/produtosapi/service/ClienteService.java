package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.CadastroDAO;
import br.com.triboraizes.produtosapi.dao.CadastroDAO.Consulta;
import br.com.triboraizes.produtosapi.dto.ClienteRequestDTO;
import br.com.triboraizes.produtosapi.dto.InteracaoRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.evento.EventoDeNegocio;
import br.com.triboraizes.produtosapi.exception.RegistroNaoEncontradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.Cliente;
import br.com.triboraizes.produtosapi.model.Interacao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cadastro de clientes do CRM, também usados como pagadores nas contas a receber. */
@Service
public class ClienteService {

    private static final Logger log = LoggerFactory.getLogger(ClienteService.class);

    private final CadastroDAO<Cliente> clientes;
    private final InteracaoService interacoes;
    private final ApplicationEventPublisher eventos;

    public ClienteService(DAOFactory fabrica, InteracaoService interacoes, ApplicationEventPublisher eventos) {
        this.clientes = fabrica.criarClienteDAO();
        this.interacoes = interacoes;
        this.eventos = eventos;
    }

    public PaginaDTO<Cliente> listar(String busca, Boolean ativo, String tipo, UUID responsavelId, int pagina,
                                     int tamanho) {
        Entradas.validarPagina(pagina, tamanho);
        Consulta consulta = Consulta.de("nome.asc,id.asc", pagina, tamanho);
        if (ativo != null) {
            consulta.onde("ativo", "is." + ativo);
        }
        if ("PF".equals(tipo) || "PJ".equals(tipo)) {
            consulta.onde("tipo", "eq." + tipo);
        }
        if (responsavelId != null) {
            consulta.onde("responsavel_id", "eq." + responsavelId);
        }
        String termo = Entradas.termo(busca);
        if (!termo.isEmpty()) {
            String grupo = Entradas.buscaEm(termo, "nome", "email", "telefone", "cidade");
            String digitos = termo.replaceAll("\\D", "");
            consulta.onde("or", digitos.length() >= 3 ? grupo.replace(")", ",documento.ilike.*" + digitos + "*)") : grupo);
        }
        CadastroDAO.Pagina<Cliente> resultado = clientes.listar(consulta);
        return PaginaDTO.of(resultado.itens(), c -> c, pagina, tamanho, resultado.total());
    }

    /** Clientes ativos para os campos de seleção (contas a receber, reclamações). */
    public List<Cliente> opcoes() {
        return clientes.todos(Map.of("ativo", "is.true"), "nome.asc");
    }

    public Cliente buscar(UUID id) {
        return clientes.buscar(id).orElseThrow(() -> naoEncontrado(id));
    }

    public Cliente criar(ClienteRequestDTO request, UUID usuario) {
        Map<String, Object> dados = dadosDo(request);
        dados.put("criado_por", usuario);
        Cliente criado = clientes.criar(dados);
        log.info("Cliente cadastrado: id={}, por={}", criado.id(), usuario);
        eventos.publishEvent(new EventoDeNegocio(EventoDeNegocio.CLIENTE_CRIADO, criado));
        return criado;
    }

    public Cliente atualizar(UUID id, ClienteRequestDTO request) {
        return clientes.atualizar(id, dadosDo(request)).orElseThrow(() -> naoEncontrado(id));
    }

    public void excluir(UUID id, UUID usuario) {
        if (!clientes.excluir(id)) {
            throw naoEncontrado(id);
        }
        log.info("Cliente excluído: id={}, por={}", id, usuario);
    }

    public List<Interacao> interacoes(UUID id) {
        buscar(id);
        return interacoes.doCliente(id);
    }

    public Interacao registrarInteracao(UUID id, InteracaoRequestDTO request, UUID usuario) {
        buscar(id);
        return interacoes.registrarNoCliente(id, request, usuario);
    }

    private static Map<String, Object> dadosDo(ClienteRequestDTO request) {
        String documento = Entradas.digitos(request.documento());
        String tipo = request.tipo() == null || request.tipo().isBlank() ? "PF" : request.tipo();
        if (documento != null && documento.length() != ("PJ".equals(tipo) ? 14 : 11)) {
            throw new RequisicaoInvalidaException("PJ".equals(tipo)
                    ? "O CNPJ deve ter 14 dígitos" : "O CPF deve ter 11 dígitos");
        }
        Map<String, Object> dados = new HashMap<>();
        dados.put("tipo", tipo);
        dados.put("nome", request.nome().strip());
        dados.put("documento", documento);
        dados.put("email", Entradas.minusculo(request.email()));
        dados.put("telefone", Entradas.opcional(request.telefone()));
        dados.put("data_nascimento", request.dataNascimento() == null ? null : request.dataNascimento().toString());
        dados.put("cep", Entradas.digitos(request.cep()));
        dados.put("endereco", Entradas.opcional(request.endereco()));
        dados.put("cidade", Entradas.opcional(request.cidade()));
        dados.put("uf", Entradas.maiusculo(request.uf()));
        dados.put("origem", Entradas.opcional(request.origem()));
        dados.put("responsavel_id", request.responsavelId());
        dados.put("observacoes", Entradas.opcional(request.observacoes()));
        dados.put("ativo", request.ativo() == null || request.ativo());
        return dados;
    }

    private static RegistroNaoEncontradoException naoEncontrado(UUID id) {
        return new RegistroNaoEncontradoException("Cliente não encontrado", "Cliente " + id + " não encontrado");
    }
}
