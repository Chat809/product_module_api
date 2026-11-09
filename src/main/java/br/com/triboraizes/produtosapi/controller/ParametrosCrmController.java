package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.EtapaRequestDTO;
import br.com.triboraizes.produtosapi.dto.OrigemRequestDTO;
import br.com.triboraizes.produtosapi.model.CrmEtapa;
import br.com.triboraizes.produtosapi.model.CrmOrigem;
import br.com.triboraizes.produtosapi.service.ParametrosCrmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/crm/parametros")
@Tag(name = "Parâmetros do CRM", description = "Etapas do funil e origens de leads. Leitura com o CRM; gravação com "
        + "Configurações > Parâmetros do CRM. Itens em uso não podem ser excluídos (409): inative-os.")
public class ParametrosCrmController {

    private final ParametrosCrmService service;

    public ParametrosCrmController(ParametrosCrmService service) {
        this.service = service;
    }

    @GetMapping("/etapas")
    @Operation(summary = "Etapas do funil, na ordem")
    public List<CrmEtapa> etapas() {
        return service.etapas();
    }

    @PostMapping("/etapas")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma etapa")
    public CrmEtapa criarEtapa(@Valid @RequestBody EtapaRequestDTO request) {
        return service.criarEtapa(request);
    }

    @PutMapping("/etapas/{id}")
    @Operation(summary = "Atualiza uma etapa")
    public CrmEtapa atualizarEtapa(@PathVariable UUID id, @Valid @RequestBody EtapaRequestDTO request) {
        return service.atualizarEtapa(id, request);
    }

    @DeleteMapping("/etapas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma etapa sem leads")
    public void excluirEtapa(@PathVariable UUID id) {
        service.excluirEtapa(id);
    }

    @GetMapping("/origens")
    @Operation(summary = "Origens de leads")
    public List<CrmOrigem> origens() {
        return service.origens();
    }

    @PostMapping("/origens")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma origem")
    public CrmOrigem criarOrigem(@Valid @RequestBody OrigemRequestDTO request) {
        return service.criarOrigem(request);
    }

    @PutMapping("/origens/{id}")
    @Operation(summary = "Atualiza uma origem")
    public CrmOrigem atualizarOrigem(@PathVariable UUID id, @Valid @RequestBody OrigemRequestDTO request) {
        return service.atualizarOrigem(id, request);
    }

    @DeleteMapping("/origens/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma origem")
    public void excluirOrigem(@PathVariable UUID id) {
        service.excluirOrigem(id);
    }
}
