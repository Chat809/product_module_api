package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.MovimentacaoRequestDTO;
import br.com.triboraizes.produtosapi.dto.MovimentacaoResponseDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.service.MovimentacaoService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.DADOS_INVALIDOS;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.ESTOQUE_INSUFICIENTE;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.NAO_ENCONTRADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.PROBLEM_JSON;

@RestController
@RequestMapping("/api/v1/produtos/{produtoId}/movimentacoes")
@Tag(name = "Movimentações de estoque", description = "Entradas, saídas e histórico do estoque de um produto")
public class MovimentacaoController {

    private final MovimentacaoService movimentacaoService;

    public MovimentacaoController(MovimentacaoService movimentacaoService) {
        this.movimentacaoService = movimentacaoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra uma entrada ou saída de estoque",
            description = "Atualiza o estoque do produto e grava a movimentação com os saldos anterior e posterior. "
                    + "O tipo AJUSTE é reservado para edições feitas pelo PUT do produto.")
    @ApiResponse(responseCode = "201", description = "Movimentação registrada")
    @ApiResponse(responseCode = "400", description = "Campos inválidos ou tipo AJUSTE",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = DADOS_INVALIDOS)))
    @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = NAO_ENCONTRADO)))
    @ApiResponse(responseCode = "422", description = "Saída maior que o estoque disponível",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = ESTOQUE_INSUFICIENTE)))
    public MovimentacaoResponseDTO registrar(@PathVariable UUID produtoId,
                                             @Valid @RequestBody MovimentacaoRequestDTO request) {
        return movimentacaoService.registrar(produtoId, request);
    }

    @GetMapping
    @Operation(summary = "Lista o histórico de movimentações do produto, das mais recentes para as mais antigas")
    @ApiResponse(responseCode = "200", description = "Página do histórico")
    @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = NAO_ENCONTRADO)))
    public PaginaDTO<MovimentacaoResponseDTO> listar(
            @PathVariable UUID produtoId,
            @Parameter(description = "Índice da página, começando em 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (1 a 100)") @RequestParam(defaultValue = "20") int size) {
        return movimentacaoService.listar(produtoId, page, size);
    }
}
