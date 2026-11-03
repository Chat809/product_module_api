package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoRequestDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoResponseDTO;
import br.com.triboraizes.produtosapi.dto.ResumoEstoqueDTO;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.CONFLITO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.DADOS_INVALIDOS;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.NAO_ENCONTRADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.PARAMETRO_INVALIDO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.PROBLEM_JSON;

@RestController
@RequestMapping("/api/v1/produtos")
@Tag(name = "Produtos", description = "Cadastro, consulta, edição e remoção de produtos")
public class ProdutoController {

    /** Aceita {@code "3"} e a forma fraca {@code W/"3"}. */
    private static final Pattern ETAG_DE_VERSAO = Pattern.compile("^(?:W/)?\"(\\d+)\"$");

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    @Operation(summary = "Lista os produtos com paginação, busca e ordenação",
            description = "A busca ignora maiúsculas e procura no nome e na descrição.")
    @ApiResponse(responseCode = "200", description = "Página de produtos")
    @ApiResponse(responseCode = "400", description = "Parâmetro de paginação ou ordenação inválido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = PARAMETRO_INVALIDO)))
    public PaginaDTO<ProdutoResponseDTO> listar(
            @Parameter(description = "Trecho do nome ou da descrição", example = "mouse")
            @RequestParam(required = false) String busca,
            @Parameter(description = "Índice da página, começando em 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (1 a 500)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "coluna,asc|desc. Colunas: nome, preco, quantidadeEmEstoque, criadoEm, atualizadoEm",
                    example = "preco,desc")
            @RequestParam(defaultValue = "nome,asc") String sort) {
        return produtoService.listar(busca, page, size, sort);
    }

    @GetMapping("/resumo")
    @Operation(summary = "Indicadores do estoque",
            description = "Totais de produtos e itens, valor total em estoque, esgotados e produtos com estoque baixo.")
    @ApiResponse(responseCode = "200", description = "Indicadores calculados no momento da consulta")
    public ResumoEstoqueDTO resumo() {
        return produtoService.resumo();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um produto pelo id")
    @ApiResponse(responseCode = "200", description = "Produto encontrado",
            headers = @Header(name = HttpHeaders.ETAG, description = "Versão atual do produto, para usar no If-Match"))
    @ApiResponse(responseCode = "400", description = "O id não é um UUID válido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = NAO_ENCONTRADO)))
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable UUID id) {
        return comEtag(ResponseEntity.ok(), produtoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cria um novo produto",
            description = "Se a quantidade inicial for maior que zero, registra uma entrada \"Estoque inicial\" no histórico.")
    @ApiResponse(responseCode = "201", description = "Produto criado",
            headers = {
                    @Header(name = HttpHeaders.LOCATION, description = "URL do produto criado"),
                    @Header(name = HttpHeaders.ETAG, description = "Versão inicial do produto")
            })
    @ApiResponse(responseCode = "400", description = "Campos inválidos ou JSON malformado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = DADOS_INVALIDOS)))
    public ResponseEntity<ProdutoResponseDTO> criar(@Valid @RequestBody ProdutoRequestDTO request) {
        ProdutoResponseDTO criado = produtoService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return comEtag(ResponseEntity.created(location), criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza todos os dados de um produto existente",
            description = """
                    Para não sobrescrever a alteração de outra pessoa, envie a versão lida no cabeçalho \
                    `If-Match` (valor do `ETag`) ou no campo `versao` do corpo. Se as duas vierem, vale o cabeçalho. \
                    Sem nenhuma das duas, a atualização é feita sem checagem. \
                    Se a quantidade mudar, um ajuste é registrado no histórico de movimentações.""")
    @ApiResponse(responseCode = "200", description = "Produto atualizado",
            headers = @Header(name = HttpHeaders.ETAG, description = "Nova versão do produto"))
    @ApiResponse(responseCode = "400", description = "Campos inválidos ou If-Match malformado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = DADOS_INVALIDOS)))
    @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = NAO_ENCONTRADO)))
    @ApiResponse(responseCode = "409", description = "O produto foi alterado depois da versão informada",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = CONFLITO)))
    public ResponseEntity<ProdutoResponseDTO> atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ProdutoRequestDTO request,
            @Parameter(description = "ETag lido no GET, por exemplo \"3\"", example = "\"0\"")
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch) {
        ProdutoRequestDTO comVersao = ifMatch == null ? request : request.comVersao(versaoDoIfMatch(ifMatch));
        return comEtag(ResponseEntity.ok(), produtoService.atualizar(id, comVersao));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um produto e o seu histórico de movimentações")
    @ApiResponse(responseCode = "204", description = "Produto removido")
    @ApiResponse(responseCode = "404", description = "Produto não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = NAO_ENCONTRADO)))
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        produtoService.remover(id);
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<ProdutoResponseDTO> comEtag(ResponseEntity.BodyBuilder builder,
                                                             ProdutoResponseDTO produto) {
        return builder.eTag("\"" + produto.versao() + "\"").body(produto);
    }

    /** {@code *} significa "qualquer versão", ou seja, sem checagem. */
    private static Long versaoDoIfMatch(String ifMatch) {
        String valor = ifMatch.strip();
        if (valor.equals("*")) {
            return null;
        }
        Matcher matcher = ETAG_DE_VERSAO.matcher(valor);
        if (!matcher.matches()) {
            throw new RequisicaoInvalidaException(
                    "O cabeçalho If-Match deve conter o ETag do produto, por exemplo \"3\"");
        }
        return Long.valueOf(matcher.group(1));
    }
}
