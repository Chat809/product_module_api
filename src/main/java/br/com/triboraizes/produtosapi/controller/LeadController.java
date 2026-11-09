package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.InteracaoRequestDTO;
import br.com.triboraizes.produtosapi.dto.LeadRequestDTO;
import br.com.triboraizes.produtosapi.dto.MoverLeadRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.model.Interacao;
import br.com.triboraizes.produtosapi.model.Lead;
import br.com.triboraizes.produtosapi.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
@RequestMapping("/api/v1/crm/leads")
@Tag(name = "Leads", description = "Funil de vendas. Exige a subaba Leads do CRM.")
public class LeadController {

    private final LeadService service;

    public LeadController(LeadService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista leads do mais novo para o mais antigo, com busca e filtros")
    public PaginaDTO<Lead> listar(@RequestParam(required = false) String busca,
                                  @RequestParam(required = false) UUID etapaId,
                                  @RequestParam(required = false) UUID origemId,
                                  @RequestParam(required = false) UUID responsavelId,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return service.listar(busca, etapaId, origemId, responsavelId, page, size);
    }

    @GetMapping("/funil")
    @Operation(summary = "Leads do funil na ordem dos cards (até 1000)")
    public List<Lead> funil(@RequestParam(required = false) String busca,
                            @RequestParam(required = false) UUID responsavelId) {
        return service.funil(busca, responsavelId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um lead")
    public Lead buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um lead (sem etapa, entra na primeira etapa aberta)")
    public Lead criar(@Valid @RequestBody LeadRequestDTO request, Authentication autenticacao) {
        return service.criar(request, UUID.fromString(autenticacao.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um lead (a etapa muda pelo PATCH /etapa)")
    public Lead atualizar(@PathVariable UUID id, @Valid @RequestBody LeadRequestDTO request) {
        return service.atualizar(id, request);
    }

    @PatchMapping("/{id}/etapa")
    @Operation(summary = "Move o lead no funil; etapas de perda exigem o motivo")
    public Lead mover(@PathVariable UUID id, @Valid @RequestBody MoverLeadRequestDTO request,
                      Authentication autenticacao) {
        return service.mover(id, request, UUID.fromString(autenticacao.getName()));
    }

    @PostMapping("/{id}/converter")
    @Operation(summary = "Converte o lead em cliente (422 se já convertido)")
    public Lead converter(@PathVariable UUID id, Authentication autenticacao) {
        return service.converter(id, UUID.fromString(autenticacao.getName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um lead e suas interações")
    public void excluir(@PathVariable UUID id, Authentication autenticacao) {
        service.excluir(id, UUID.fromString(autenticacao.getName()));
    }

    @GetMapping("/{id}/interacoes")
    @Operation(summary = "Interações com o lead, da mais recente para a mais antiga")
    public List<Interacao> interacoes(@PathVariable UUID id) {
        return service.interacoes(id);
    }

    @PostMapping("/{id}/interacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma interação com o lead")
    public Interacao registrarInteracao(@PathVariable UUID id, @Valid @RequestBody InteracaoRequestDTO request,
                                        Authentication autenticacao) {
        return service.registrarInteracao(id, request, UUID.fromString(autenticacao.getName()));
    }
}
