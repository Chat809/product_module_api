package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoRequestDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoResponseDTO;
import br.com.triboraizes.produtosapi.dto.ResumoEstoqueDTO;
import br.com.triboraizes.produtosapi.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/produtos")
@Tag(name = "Produtos", description = "Gerenciamento de produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    @Operation(summary = "Lista os produtos com paginação, busca e ordenação")
    public PaginaDTO<ProdutoResponseDTO> listar(
            @Parameter(description = "Trecho do nome ou da descrição") @RequestParam(required = false) String busca,
            @Parameter(description = "Índice da página, começando em 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (1 a 500)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "coluna,asc|desc. Colunas: nome, preco, quantidadeEmEstoque, criadoEm, atualizadoEm")
            @RequestParam(defaultValue = "nome,asc") String sort) {
        return produtoService.listar(busca, page, size, sort);
    }

    @GetMapping("/resumo")
    @Operation(summary = "Indicadores do estoque: totais, valor em estoque, esgotados e estoque baixo")
    public ResumoEstoqueDTO resumo() {
        return produtoService.resumo();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um produto pelo id")
    public ProdutoResponseDTO buscarPorId(@PathVariable UUID id) {
        return produtoService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Cria um novo produto")
    public ResponseEntity<ProdutoResponseDTO> criar(@Valid @RequestBody ProdutoRequestDTO request) {
        ProdutoResponseDTO criado = produtoService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza todos os dados de um produto existente")
    public ProdutoResponseDTO atualizar(@PathVariable UUID id, @Valid @RequestBody ProdutoRequestDTO request) {
        return produtoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um produto")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        produtoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
