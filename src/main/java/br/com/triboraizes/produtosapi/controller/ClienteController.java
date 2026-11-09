package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.ClienteRequestDTO;
import br.com.triboraizes.produtosapi.dto.InteracaoRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.model.Cliente;
import br.com.triboraizes.produtosapi.model.Interacao;
import br.com.triboraizes.produtosapi.model.Reclamacao;
import br.com.triboraizes.produtosapi.service.ClienteService;
import br.com.triboraizes.produtosapi.service.ReclamacaoService;
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
@RequestMapping("/api/v1/crm/clientes")
@Tag(name = "Clientes", description = "Leitura do cadastro com o CRM ou o financeiro; ficha (interações e reclamações) "
        + "com o CRM; gravação com a subaba Clientes. Clientes com reclamações ou contas não podem ser excluídos (409).")
public class ClienteController {

    private final ClienteService service;
    private final ReclamacaoService reclamacoes;

    public ClienteController(ClienteService service, ReclamacaoService reclamacoes) {
        this.service = service;
        this.reclamacoes = reclamacoes;
    }

    @GetMapping
    @Operation(summary = "Lista clientes por nome, com busca (nome, e-mail, telefone, cidade, documento) e filtros")
    public PaginaDTO<Cliente> listar(@RequestParam(required = false) String busca,
                                     @RequestParam(required = false) Boolean ativo,
                                     @RequestParam(required = false) String tipo,
                                     @RequestParam(required = false) UUID responsavelId,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return service.listar(busca, ativo, tipo, responsavelId, page, size);
    }

    @GetMapping("/opcoes")
    @Operation(summary = "Clientes ativos, para campos de seleção")
    public List<Cliente> opcoes() {
        return service.opcoes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um cliente")
    public Cliente buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um cliente (CPF/CNPJ único)")
    public Cliente criar(@Valid @RequestBody ClienteRequestDTO request, Authentication autenticacao) {
        return service.criar(request, UUID.fromString(autenticacao.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um cliente")
    public Cliente atualizar(@PathVariable UUID id, @Valid @RequestBody ClienteRequestDTO request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um cliente sem reclamações nem contas")
    public void excluir(@PathVariable UUID id, Authentication autenticacao) {
        service.excluir(id, UUID.fromString(autenticacao.getName()));
    }

    @GetMapping("/{id}/interacoes")
    @Operation(summary = "Interações com o cliente, da mais recente para a mais antiga")
    public List<Interacao> interacoes(@PathVariable UUID id) {
        return service.interacoes(id);
    }

    @PostMapping("/{id}/interacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma interação com o cliente")
    public Interacao registrarInteracao(@PathVariable UUID id, @Valid @RequestBody InteracaoRequestDTO request,
                                        Authentication autenticacao) {
        return service.registrarInteracao(id, request, UUID.fromString(autenticacao.getName()));
    }

    @GetMapping("/{id}/reclamacoes")
    @Operation(summary = "Reclamações do cliente")
    public List<Reclamacao> reclamacoes(@PathVariable UUID id) {
        service.buscar(id);
        return reclamacoes.doCliente(id);
    }
}
