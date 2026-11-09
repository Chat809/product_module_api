package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.dao.CadastroDAO;
import br.com.triboraizes.produtosapi.dao.CadastroDAO.Consulta;
import br.com.triboraizes.produtosapi.dto.CentroCustoRequestDTO;
import br.com.triboraizes.produtosapi.dto.FornecedorRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.exception.RegistroNaoEncontradoException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.factory.DAOFactory;
import br.com.triboraizes.produtosapi.model.CentroCusto;
import br.com.triboraizes.produtosapi.model.Fornecedor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Fornecedores e centros de custo, usados nos lançamentos do financeiro e nos relatórios. */
@Service
public class CadastroFinanceiroService {

    private static final Logger log = LoggerFactory.getLogger(CadastroFinanceiroService.class);

    private final CadastroDAO<Fornecedor> fornecedores;
    private final CadastroDAO<CentroCusto> centros;

    public CadastroFinanceiroService(DAOFactory fabrica) {
        this.fornecedores = fabrica.criarFornecedorDAO();
        this.centros = fabrica.criarCentroCustoDAO();
    }

    public PaginaDTO<Fornecedor> listarFornecedores(String busca, Boolean ativo, int pagina, int tamanho) {
        Entradas.validarPagina(pagina, tamanho);
        Consulta consulta = Consulta.de("razao_social.asc,id.asc", pagina, tamanho);
        filtrarAtivo(consulta, ativo);
        String termo = Entradas.termo(busca);
        if (!termo.isEmpty()) {
            String digitos = termo.replaceAll("\\D", "");
            consulta.onde("or", digitos.length() >= 3
                    ? Entradas.buscaEm(termo, "razao_social", "nome_fantasia", "categoria", "email")
                            .replace(")", ",documento.ilike.*" + digitos + "*)")
                    : Entradas.buscaEm(termo, "razao_social", "nome_fantasia", "categoria", "email"));
        }
        CadastroDAO.Pagina<Fornecedor> resultado = fornecedores.listar(consulta);
        return PaginaDTO.of(resultado.itens(), f -> f, pagina, tamanho, resultado.total());
    }

    /** Fornecedores ativos para os campos de seleção dos lançamentos. */
    public List<Fornecedor> opcoesDeFornecedores() {
        return fornecedores.todos(Map.of("ativo", "is.true"), "razao_social.asc");
    }

    public Fornecedor buscarFornecedor(UUID id) {
        return fornecedores.buscar(id).orElseThrow(() -> fornecedorNaoEncontrado(id));
    }

    public Fornecedor criarFornecedor(FornecedorRequestDTO request, UUID usuario) {
        Map<String, Object> dados = dadosDoFornecedor(request);
        dados.put("criado_por", usuario);
        Fornecedor criado = fornecedores.criar(dados);
        log.info("Fornecedor cadastrado: id={}, por={}", criado.id(), usuario);
        return criado;
    }

    public Fornecedor atualizarFornecedor(UUID id, FornecedorRequestDTO request) {
        return fornecedores.atualizar(id, dadosDoFornecedor(request)).orElseThrow(() -> fornecedorNaoEncontrado(id));
    }

    public void excluirFornecedor(UUID id, UUID usuario) {
        if (!fornecedores.excluir(id)) {
            throw fornecedorNaoEncontrado(id);
        }
        log.info("Fornecedor excluído: id={}, por={}", id, usuario);
    }

    public PaginaDTO<CentroCusto> listarCentros(String busca, Boolean ativo, int pagina, int tamanho) {
        Entradas.validarPagina(pagina, tamanho);
        Consulta consulta = Consulta.de("codigo.asc", pagina, tamanho);
        filtrarAtivo(consulta, ativo);
        String termo = Entradas.termo(busca);
        if (!termo.isEmpty()) {
            consulta.onde("or", Entradas.buscaEm(termo, "codigo", "nome", "descricao"));
        }
        CadastroDAO.Pagina<CentroCusto> resultado = centros.listar(consulta);
        return PaginaDTO.of(resultado.itens(), c -> c, pagina, tamanho, resultado.total());
    }

    public List<CentroCusto> opcoesDeCentros() {
        return centros.todos(Map.of("ativo", "is.true"), "codigo.asc");
    }

    public CentroCusto criarCentro(CentroCustoRequestDTO request) {
        return centros.criar(dadosDoCentro(request));
    }

    public CentroCusto atualizarCentro(UUID id, CentroCustoRequestDTO request) {
        return centros.atualizar(id, dadosDoCentro(request)).orElseThrow(() -> centroNaoEncontrado(id));
    }

    public void excluirCentro(UUID id) {
        if (!centros.excluir(id)) {
            throw centroNaoEncontrado(id);
        }
    }

    private static Map<String, Object> dadosDoFornecedor(FornecedorRequestDTO request) {
        String documento = Entradas.digitos(request.documento());
        if (documento != null && documento.length() != 11 && documento.length() != 14) {
            throw new RequisicaoInvalidaException("O documento deve ser um CPF (11 dígitos) ou CNPJ (14 dígitos)");
        }
        Map<String, Object> dados = new HashMap<>();
        dados.put("razao_social", request.razaoSocial().strip());
        dados.put("nome_fantasia", Entradas.opcional(request.nomeFantasia()));
        dados.put("documento", documento);
        dados.put("contato", Entradas.opcional(request.contato()));
        dados.put("email", Entradas.minusculo(request.email()));
        dados.put("telefone", Entradas.opcional(request.telefone()));
        dados.put("chave_pix", Entradas.opcional(request.chavePix()));
        dados.put("categoria", Entradas.opcional(request.categoria()));
        dados.put("observacoes", Entradas.opcional(request.observacoes()));
        dados.put("ativo", request.ativo() == null || request.ativo());
        return dados;
    }

    private static Map<String, Object> dadosDoCentro(CentroCustoRequestDTO request) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("codigo", Entradas.maiusculo(request.codigo()));
        dados.put("nome", request.nome().strip());
        dados.put("descricao", Entradas.opcional(request.descricao()));
        dados.put("ativo", request.ativo() == null || request.ativo());
        return dados;
    }

    private static void filtrarAtivo(Consulta consulta, Boolean ativo) {
        if (ativo != null) {
            consulta.onde("ativo", "is." + ativo);
        }
    }

    private static RegistroNaoEncontradoException fornecedorNaoEncontrado(UUID id) {
        return new RegistroNaoEncontradoException("Fornecedor não encontrado", "Fornecedor " + id + " não encontrado");
    }

    private static RegistroNaoEncontradoException centroNaoEncontrado(UUID id) {
        return new RegistroNaoEncontradoException("Centro de custo não encontrado",
                "Centro de custo " + id + " não encontrado");
    }
}
