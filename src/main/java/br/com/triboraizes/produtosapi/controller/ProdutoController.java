package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.ProdutoRequestDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoResponseDTO;
import br.com.triboraizes.produtosapi.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
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
    @Operation(summary = "Lista todos os produtos")
    public List<ProdutoResponseDTO> listarTodos() {
        return produtoService.listarTodos();
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
