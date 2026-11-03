package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.DemandaDTO;
import br.com.triboraizes.produtosapi.dto.DemandaRequestDTO;
import br.com.triboraizes.produtosapi.dto.MoverDemandaRequestDTO;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.model.FiltroDemandas;
import br.com.triboraizes.produtosapi.model.StatusDemanda;
import br.com.triboraizes.produtosapi.service.DemandaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
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

import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.ACESSO_NEGADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.DADOS_INDISPONIVEIS;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.DADOS_INVALIDOS;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.DEMANDA_NAO_ENCONTRADA;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.EXCLUSAO_DE_DEMANDA;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.NAO_AUTENTICADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.PROBLEM_JSON;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.RESPONSAVEL_INVALIDO;

@RestController
@RequestMapping("/api/v1/demandas")
@Tag(name = "Demandas", description = "Kanban de demandas. As demandas abertas em que o usuário é responsável são as tarefas dele. "
        + "Exige o módulo 'demandas'.")
@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = NAO_AUTENTICADO)))
@ApiResponse(responseCode = "403", description = "Usuário sem o módulo de demandas",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = ACESSO_NEGADO)))
@ApiResponse(responseCode = "503", description = "Banco indisponível",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = DADOS_INDISPONIVEIS)))
public class DemandaController {

    private static final String EU = "me";

    private final DemandaService demandaService;

    public DemandaController(DemandaService demandaService) {
        this.demandaService = demandaService;
    }

    @GetMapping
    @Operation(summary = "Lista as demandas na ordem do Kanban (até 500)",
            description = "Use responsavel=me&abertas=true para as tarefas do usuário logado.")
    @ApiResponse(responseCode = "200", description = "Demandas encontradas")
    public List<DemandaDTO> listar(
            @Parameter(description = "'me' ou o id de um usuário") @RequestParam(required = false) String responsavel,
            @Parameter(description = "Coluna do Kanban") @RequestParam(required = false) StatusDemanda status,
            @Parameter(description = "Trecho do título ou da descrição") @RequestParam(required = false) String busca,
            @Parameter(description = "Só demandas não concluídas") @RequestParam(defaultValue = "false") boolean abertas,
            Authentication autenticacao) {
        return demandaService.listar(new FiltroDemandas(responsavel(responsavel, autenticacao), status, busca, abertas));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma demanda pelo id")
    @ApiResponse(responseCode = "200", description = "Demanda encontrada")
    @ApiResponse(responseCode = "404", description = "Demanda não encontrada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = DEMANDA_NAO_ENCONTRADA)))
    public DemandaDTO buscar(@PathVariable UUID id) {
        return demandaService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma demanda no fim da coluna (padrão A_FAZER)")
    @ApiResponse(responseCode = "201", description = "Demanda criada")
    @ApiResponse(responseCode = "400", description = "Campos inválidos ou responsável inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = {@ExampleObject(name = "Campos", value = DADOS_INVALIDOS),
                            @ExampleObject(name = "Responsável", value = RESPONSAVEL_INVALIDO)}))
    public DemandaDTO criar(@Valid @RequestBody DemandaRequestDTO request, Authentication autenticacao) {
        return demandaService.criar(request, usuario(autenticacao));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita uma demanda (qualquer usuário com o módulo)")
    @ApiResponse(responseCode = "200", description = "Demanda atualizada")
    @ApiResponse(responseCode = "400", description = "Campos inválidos ou responsável inexistente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = RESPONSAVEL_INVALIDO)))
    @ApiResponse(responseCode = "404", description = "Demanda não encontrada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = DEMANDA_NAO_ENCONTRADA)))
    public DemandaDTO atualizar(@PathVariable UUID id, @Valid @RequestBody DemandaRequestDTO request) {
        return demandaService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Move o cartão para outra coluna e/ou posição (arrastar e soltar)")
    @ApiResponse(responseCode = "200", description = "Demanda movida")
    @ApiResponse(responseCode = "404", description = "Demanda não encontrada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = DEMANDA_NAO_ENCONTRADA)))
    public DemandaDTO mover(@PathVariable UUID id, @Valid @RequestBody MoverDemandaRequestDTO request) {
        return demandaService.mover(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma demanda (só quem criou ou um administrador)")
    @ApiResponse(responseCode = "204", description = "Demanda excluída")
    @ApiResponse(responseCode = "404", description = "Demanda não encontrada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = DEMANDA_NAO_ENCONTRADA)))
    @ApiResponse(responseCode = "422", description = "Usuário não criou a demanda e não é administrador",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = EXCLUSAO_DE_DEMANDA)))
    public void excluir(@PathVariable UUID id, Authentication autenticacao) {
        boolean administrador = autenticacao.getAuthorities().stream()
                .anyMatch(autoridade -> "ROLE_ADMIN".equals(autoridade.getAuthority()));
        demandaService.excluir(id, usuario(autenticacao), administrador);
    }

    private static UUID responsavel(String valor, Authentication autenticacao) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        if (EU.equalsIgnoreCase(valor.trim())) {
            return usuario(autenticacao);
        }
        try {
            return UUID.fromString(valor.trim());
        } catch (IllegalArgumentException ex) {
            throw new RequisicaoInvalidaException("Use 'me' ou o id de um usuário em 'responsavel'");
        }
    }

    private static UUID usuario(Authentication autenticacao) {
        return UUID.fromString(autenticacao.getName());
    }
}
