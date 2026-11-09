package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.ComentarioRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.ReclamacaoRequestDTO;
import br.com.triboraizes.produtosapi.dto.StatusReclamacaoRequestDTO;
import br.com.triboraizes.produtosapi.model.Reclamacao;
import br.com.triboraizes.produtosapi.model.ReclamacaoEvento;
import br.com.triboraizes.produtosapi.service.ReclamacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
@RequestMapping("/api/v1/crm/reclamacoes")
@Tag(name = "Reclamações", description = "Atendimento de reclamações com protocolo e histórico. Exige a subaba "
        + "Reclamações do CRM. Reclamações não são excluídas: encerre-as.")
public class ReclamacaoController {

    private final ReclamacaoService service;

    public ReclamacaoController(ReclamacaoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Lista reclamações da mais nova para a mais antiga, com busca e filtros")
    public PaginaDTO<Reclamacao> listar(@RequestParam(required = false) String busca,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String prioridade,
                                        @RequestParam(required = false) UUID responsavelId,
                                        @RequestParam(required = false) UUID clienteId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return service.listar(busca, status, prioridade, responsavelId, clienteId, page, size);
    }

    @GetMapping("/quadro")
    @Operation(summary = "Reclamações para o quadro por status (até 1000)")
    public List<Reclamacao> quadro(@RequestParam(required = false) String busca,
                                   @RequestParam(required = false) UUID responsavelId) {
        return service.quadro(busca, responsavelId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma reclamação")
    public Reclamacao buscar(@PathVariable UUID id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Abre uma reclamação; o protocolo é gerado automaticamente")
    public Reclamacao abrir(@Valid @RequestBody ReclamacaoRequestDTO request, Authentication autenticacao) {
        return service.abrir(request, UUID.fromString(autenticacao.getName()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de uma reclamação (o status muda pelo PATCH /status)")
    public Reclamacao atualizar(@PathVariable UUID id, @Valid @RequestBody ReclamacaoRequestDTO request,
                                Authentication autenticacao) {
        return service.atualizar(id, request, UUID.fromString(autenticacao.getName()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Muda o status; resolver exige a solução. A mudança entra no histórico")
    public Reclamacao alterarStatus(@PathVariable UUID id, @Valid @RequestBody StatusReclamacaoRequestDTO request,
                                    Authentication autenticacao) {
        return service.alterarStatus(id, request, UUID.fromString(autenticacao.getName()));
    }

    @GetMapping("/{id}/historico")
    @Operation(summary = "Histórico da reclamação, do mais antigo para o mais recente")
    public List<ReclamacaoEvento> historico(@PathVariable UUID id) {
        return service.historico(id);
    }

    @PostMapping("/{id}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Acrescenta um comentário ao histórico")
    public ReclamacaoEvento comentar(@PathVariable UUID id, @Valid @RequestBody ComentarioRequestDTO request,
                                     Authentication autenticacao) {
        return service.comentar(id, request, UUID.fromString(autenticacao.getName()));
    }
}
