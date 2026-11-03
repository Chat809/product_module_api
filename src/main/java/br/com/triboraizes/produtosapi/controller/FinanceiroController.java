package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.BaixaRequestDTO;
import br.com.triboraizes.produtosapi.dto.ContaDTO;
import br.com.triboraizes.produtosapi.dto.ContaRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.ResumoFinanceiroDTO;
import br.com.triboraizes.produtosapi.model.StatusConta;
import br.com.triboraizes.produtosapi.model.TipoConta;
import br.com.triboraizes.produtosapi.service.ContaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.UUID;

import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.ACESSO_NEGADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.CHAVE_PIX_OBRIGATORIA;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.CONTA_JA_QUITADA;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.CONTA_NAO_ENCONTRADA;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.DADOS_INDISPONIVEIS;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.DADOS_INVALIDOS;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.NAO_AUTENTICADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.PARAMETRO_INVALIDO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.PROBLEM_JSON;

@RestController
@RequestMapping("/api/v1/financeiro")
@Tag(name = "Financeiro", description = "Contas a pagar e a receber com imposto, forma de pagamento e chave Pix. "
        + "As duas listas têm os mesmos endpoints, trocando 'contas-pagar' por 'contas-receber'. Exige o módulo 'financeiro'.")
@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = NAO_AUTENTICADO)))
@ApiResponse(responseCode = "403", description = "Usuário sem o módulo financeiro",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = ACESSO_NEGADO)))
@ApiResponse(responseCode = "503", description = "Banco indisponível",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = DADOS_INDISPONIVEIS)))
public class FinanceiroController {

    private static final String TIPO = "/{tipo:contas-pagar|contas-receber}";

    private final ContaService contaService;

    public FinanceiroController(ContaService contaService) {
        this.contaService = contaService;
    }

    @GetMapping("/resumo")
    @Operation(summary = "Totais pendentes, vencidos, quitados no mês e saldo previsto (pelo valor líquido)")
    @ApiResponse(responseCode = "200", description = "Resumo financeiro")
    public ResumoFinanceiroDTO resumo() {
        return contaService.resumo();
    }

    @GetMapping(TIPO)
    @Operation(summary = "Lista as contas em ordem de vencimento, paginadas")
    @ApiResponse(responseCode = "200", description = "Página de contas")
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = PARAMETRO_INVALIDO)))
    public PaginaDTO<ContaDTO> listar(
            @Parameter(description = "contas-pagar ou contas-receber") @PathVariable String tipo,
            @Parameter(description = "PENDENTE, PAGO (a pagar), RECEBIDO (a receber) ou CANCELADO")
            @RequestParam(required = false) StatusConta status,
            @Parameter(description = "Só pendentes com vencimento antes de hoje") @RequestParam(defaultValue = "false") boolean vencidas,
            @Parameter(description = "Trecho da descrição, da pessoa ou da categoria") @RequestParam(required = false) String busca,
            @Parameter(description = "Vencimento a partir de (aaaa-mm-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimentoDe,
            @Parameter(description = "Vencimento até (aaaa-mm-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate vencimentoAte,
            @Parameter(description = "Índice da página, começando em 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (1 a 100)") @RequestParam(defaultValue = "20") int size) {
        return contaService.listar(TipoConta.doCaminho(tipo), status, vencidas, busca, vencimentoDe, vencimentoAte, page, size);
    }

    @GetMapping(TIPO + "/{id}")
    @Operation(summary = "Busca uma conta pelo id")
    @ApiResponse(responseCode = "200", description = "Conta encontrada")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = CONTA_NAO_ENCONTRADA)))
    public ContaDTO buscar(@PathVariable String tipo, @PathVariable UUID id) {
        return contaService.buscar(TipoConta.doCaminho(tipo), id);
    }

    @PostMapping(TIPO)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Lança uma conta pendente",
            description = "Imposto e valor líquido são calculados pelo banco a partir do percentual. "
                    + "A chave Pix é obrigatória quando a forma de pagamento é PIX.")
    @ApiResponse(responseCode = "201", description = "Conta lançada")
    @ApiResponse(responseCode = "400", description = "Campos inválidos ou chave Pix ausente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = {@ExampleObject(name = "Campos", value = DADOS_INVALIDOS),
                            @ExampleObject(name = "Pix", value = CHAVE_PIX_OBRIGATORIA)}))
    public ContaDTO criar(@PathVariable String tipo, @Valid @RequestBody ContaRequestDTO request,
                          Authentication autenticacao) {
        return contaService.criar(TipoConta.doCaminho(tipo), request, usuario(autenticacao));
    }

    @PutMapping(TIPO + "/{id}")
    @Operation(summary = "Edita os dados do lançamento (status e baixa têm endpoints próprios)")
    @ApiResponse(responseCode = "200", description = "Conta atualizada")
    @ApiResponse(responseCode = "400", description = "Campos inválidos ou chave Pix ausente",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = CHAVE_PIX_OBRIGATORIA)))
    @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = CONTA_NAO_ENCONTRADA)))
    public ContaDTO atualizar(@PathVariable String tipo, @PathVariable UUID id,
                              @Valid @RequestBody ContaRequestDTO request) {
        return contaService.atualizar(TipoConta.doCaminho(tipo), id, request);
    }

    @PatchMapping(TIPO + "/{id}/baixa")
    @Operation(summary = "Dá baixa: marca como PAGO (a pagar) ou RECEBIDO (a receber) com a data informada")
    @ApiResponse(responseCode = "200", description = "Baixa registrada")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = CONTA_NAO_ENCONTRADA)))
    @ApiResponse(responseCode = "422", description = "Conta já quitada ou cancelada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = CONTA_JA_QUITADA)))
    public ContaDTO baixar(@PathVariable String tipo, @PathVariable UUID id,
                           @RequestBody(required = false) BaixaRequestDTO request, Authentication autenticacao) {
        return contaService.baixar(TipoConta.doCaminho(tipo), id, request == null ? null : request.data(),
                usuario(autenticacao));
    }

    @PatchMapping(TIPO + "/{id}/cancelamento")
    @Operation(summary = "Cancela uma conta pendente, mantendo o histórico")
    @ApiResponse(responseCode = "200", description = "Conta cancelada")
    @ApiResponse(responseCode = "422", description = "Conta já quitada ou cancelada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = CONTA_JA_QUITADA)))
    public ContaDTO cancelar(@PathVariable String tipo, @PathVariable UUID id, Authentication autenticacao) {
        return contaService.cancelar(TipoConta.doCaminho(tipo), id, usuario(autenticacao));
    }

    @DeleteMapping(TIPO + "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma conta lançada por engano")
    @ApiResponse(responseCode = "204", description = "Conta excluída")
    @ApiResponse(responseCode = "404", description = "Conta não encontrada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = CONTA_NAO_ENCONTRADA)))
    public void excluir(@PathVariable String tipo, @PathVariable UUID id, Authentication autenticacao) {
        contaService.excluir(TipoConta.doCaminho(tipo), id, usuario(autenticacao));
    }

    private static UUID usuario(Authentication autenticacao) {
        return UUID.fromString(autenticacao.getName());
    }
}
