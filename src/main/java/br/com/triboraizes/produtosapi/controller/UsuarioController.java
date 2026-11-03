package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.AlterarPapelRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioDTO;
import br.com.triboraizes.produtosapi.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.ACESSO_NEGADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.NAO_AUTENTICADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.OPERACAO_NAO_PERMITIDA;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.PROBLEM_JSON;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.USUARIO_NAO_ENCONTRADO;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuários", description = "Dados do usuário logado e gestão de papéis (administradores)")
@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = NAO_AUTENTICADO)))
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    @Operation(summary = "Devolve o usuário logado, com o papel atual gravado no banco")
    @ApiResponse(responseCode = "200", description = "Usuário logado")
    public UsuarioDTO me(@AuthenticationPrincipal Jwt jwt) {
        return usuarioService.buscar(UUID.fromString(jwt.getSubject()));
    }

    @GetMapping
    @Operation(summary = "Lista os usuários em ordem alfabética (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Página de usuários")
    @ApiResponse(responseCode = "403", description = "Usuário sem papel ADMIN",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = ACESSO_NEGADO)))
    public PaginaDTO<UsuarioDTO> listar(
            @Parameter(description = "Índice da página, começando em 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página (1 a 100)") @RequestParam(defaultValue = "20") int size) {
        return usuarioService.listar(page, size);
    }

    @PatchMapping("/{id}/papel")
    @Operation(summary = "Altera o papel de um usuário (somente ADMIN)",
            description = "O usuário afetado passa a ter o novo papel no próximo login ou renovação de sessão.")
    @ApiResponse(responseCode = "200", description = "Papel alterado")
    @ApiResponse(responseCode = "403", description = "Usuário sem papel ADMIN",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = ACESSO_NEGADO)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = USUARIO_NAO_ENCONTRADO)))
    @ApiResponse(responseCode = "422", description = "Admin tentando remover o próprio acesso",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = OPERACAO_NAO_PERMITIDA)))
    public UsuarioDTO alterarPapel(@PathVariable UUID id,
                                   @Valid @RequestBody AlterarPapelRequestDTO request,
                                   @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.alterarPapel(id, request.papel(), UUID.fromString(jwt.getSubject()));
    }
}
