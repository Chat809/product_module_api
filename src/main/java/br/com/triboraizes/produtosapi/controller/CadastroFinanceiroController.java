package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.CentroCustoRequestDTO;
import br.com.triboraizes.produtosapi.dto.FornecedorRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.model.CentroCusto;
import br.com.triboraizes.produtosapi.model.Fornecedor;
import br.com.triboraizes.produtosapi.service.CadastroFinanceiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/financeiro")
@Tag(name = "Fornecedores e centros de custo", description = "Leitura com o módulo financeiro; gravação com a subaba "
        + "correspondente. Cadastros usados em lançamentos não podem ser excluídos (409): inative-os.")
public class CadastroFinanceiroController {

    private final CadastroFinanceiroService service;

    public CadastroFinanceiroController(CadastroFinanceiroService service) {
        this.service = service;
    }

    @GetMapping("/fornecedores")
    @Operation(summary = "Lista fornecedores por razão social, com busca e filtro de ativos")
    public PaginaDTO<Fornecedor> listarFornecedores(@RequestParam(required = false) String busca,
                                                    @RequestParam(required = false) Boolean ativo,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.listarFornecedores(busca, ativo, page, size);
    }

    @GetMapping("/fornecedores/opcoes")
    @Operation(summary = "Fornecedores ativos, para campos de seleção")
    public List<Fornecedor> opcoesDeFornecedores() {
        return service.opcoesDeFornecedores();
    }

    @GetMapping("/fornecedores/{id}")
    @Operation(summary = "Busca um fornecedor")
    public Fornecedor buscarFornecedor(@PathVariable UUID id) {
        return service.buscarFornecedor(id);
    }

    @PostMapping("/fornecedores")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um fornecedor (CPF/CNPJ único)")
    public Fornecedor criarFornecedor(@Valid @RequestBody FornecedorRequestDTO request, Authentication autenticacao) {
        return service.criarFornecedor(request, UUID.fromString(autenticacao.getName()));
    }

    @PutMapping("/fornecedores/{id}")
    @Operation(summary = "Atualiza um fornecedor")
    public Fornecedor atualizarFornecedor(@PathVariable UUID id, @Valid @RequestBody FornecedorRequestDTO request) {
        return service.atualizarFornecedor(id, request);
    }

    @DeleteMapping("/fornecedores/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um fornecedor sem lançamentos")
    public void excluirFornecedor(@PathVariable UUID id, Authentication autenticacao) {
        service.excluirFornecedor(id, UUID.fromString(autenticacao.getName()));
    }

    @GetMapping("/centros-custo")
    @Operation(summary = "Lista centros de custo por código")
    public PaginaDTO<CentroCusto> listarCentros(@RequestParam(required = false) String busca,
                                                @RequestParam(required = false) Boolean ativo,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return service.listarCentros(busca, ativo, page, size);
    }

    @GetMapping("/centros-custo/opcoes")
    @Operation(summary = "Centros de custo ativos, para campos de seleção")
    public List<CentroCusto> opcoesDeCentros() {
        return service.opcoesDeCentros();
    }

    @PostMapping("/centros-custo")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um centro de custo (código único)")
    public CentroCusto criarCentro(@Valid @RequestBody CentroCustoRequestDTO request) {
        return service.criarCentro(request);
    }

    @PutMapping("/centros-custo/{id}")
    @Operation(summary = "Atualiza um centro de custo")
    public CentroCusto atualizarCentro(@PathVariable UUID id, @Valid @RequestBody CentroCustoRequestDTO request) {
        return service.atualizarCentro(id, request);
    }

    @DeleteMapping("/centros-custo/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um centro de custo sem lançamentos")
    public void excluirCentro(@PathVariable UUID id) {
        service.excluirCentro(id);
    }
}
