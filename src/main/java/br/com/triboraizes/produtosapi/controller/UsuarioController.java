package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.AtualizarUsuarioRequestDTO;
import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.RedefinirSenhaRequestDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioDTO;
import br.com.triboraizes.produtosapi.dto.UsuarioOpcaoDTO;
import br.com.triboraizes.produtosapi.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.ACESSO_INVALIDO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.ACESSO_NEGADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.ARQUIVO_GRANDE;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.DADOS_INVALIDOS;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.EMAIL_JA_CADASTRADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.IMAGEM_INVALIDA;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.NAO_AUTENTICADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.OPERACAO_NAO_PERMITIDA;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.PROBLEM_JSON;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.USUARIO_NAO_ENCONTRADO;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuários", description = "Gestão de usuários, papéis e acessos (administradores)")
@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = NAO_AUTENTICADO)))
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/opcoes")
    @Operation(summary = "Lista id, nome e avatar de todos os usuários, para escolher responsáveis de demandas",
            description = "Liberado a quem tem o módulo 'demandas'. Não devolve e-mail nem telefone.")
    @ApiResponse(responseCode = "200", description = "Usuários em ordem alfabética")
    @ApiResponse(responseCode = "403", description = "Usuário sem o módulo de demandas",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = ACESSO_NEGADO)))
    public List<UsuarioOpcaoDTO> opcoes() {
        return usuarioService.opcoes();
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

    @PutMapping("/{id}")
    @Operation(summary = "Edita nome, e-mail, nível de acesso, acessos, departamento e cargo de um usuário (somente ADMIN)",
            description = "Acessos: [\"*\"] libera todos os módulos; uma lista como [\"dashboard\"] limita aos módulos "
                    + "indicados. ADMIN sempre fica com [\"*\"] e o módulo 'usuarios' é exclusivo de ADMIN. "
                    + "O usuário afetado recebe o novo papel e os novos acessos no próximo login ou renovação de sessão.")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado")
    @ApiResponse(responseCode = "400", description = "Campos inválidos ou acesso desconhecido",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = ACESSO_INVALIDO)))
    @ApiResponse(responseCode = "403", description = "Usuário sem papel ADMIN",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = ACESSO_NEGADO)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = USUARIO_NAO_ENCONTRADO)))
    @ApiResponse(responseCode = "409", description = "O novo e-mail já pertence a outra conta",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = EMAIL_JA_CADASTRADO)))
    @ApiResponse(responseCode = "422", description = "Admin tentando remover o próprio acesso de administrador",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = OPERACAO_NAO_PERMITIDA)))
    public UsuarioDTO atualizar(@PathVariable UUID id,
                                @Valid @RequestBody AtualizarUsuarioRequestDTO request,
                                @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.atualizar(id, request, UUID.fromString(jwt.getSubject()));
    }

    @PutMapping("/{id}/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Define uma nova senha para o usuário (somente ADMIN)",
            description = "As senhas ficam no Supabase Auth apenas como hash, então não podem ser consultadas, só redefinidas.")
    @ApiResponse(responseCode = "204", description = "Senha redefinida")
    @ApiResponse(responseCode = "400", description = "Senha fora das regras (6 a 72 caracteres)",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = DADOS_INVALIDOS)))
    @ApiResponse(responseCode = "403", description = "Usuário sem papel ADMIN",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = ACESSO_NEGADO)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = USUARIO_NAO_ENCONTRADO)))
    public void redefinirSenha(@PathVariable UUID id,
                               @Valid @RequestBody RedefinirSenhaRequestDTO request,
                               @AuthenticationPrincipal Jwt jwt) {
        usuarioService.redefinirSenha(id, request.senha(), UUID.fromString(jwt.getSubject()));
    }

    @PutMapping(path = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Define a foto de um usuário (somente ADMIN; PNG, JPEG ou WEBP, até 2 MB)",
            description = "Mesmas regras de PUT /api/v1/usuarios/me/avatar. A imagem anterior é apagada do Storage.")
    @ApiResponse(responseCode = "200", description = "Foto atualizada; a URL pública vem em avatarUrl")
    @ApiResponse(responseCode = "400", description = "Arquivo ausente ou formato não suportado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = IMAGEM_INVALIDA)))
    @ApiResponse(responseCode = "403", description = "Usuário sem papel ADMIN",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = ACESSO_NEGADO)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = USUARIO_NAO_ENCONTRADO)))
    @ApiResponse(responseCode = "413", description = "Arquivo maior que 2 MB",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = ARQUIVO_GRANDE)))
    public UsuarioDTO trocarAvatar(@PathVariable UUID id,
                                   @Parameter(description = "Imagem da foto") @RequestPart("arquivo") MultipartFile arquivo,
                                   @AuthenticationPrincipal Jwt jwt) throws IOException {
        return usuarioService.trocarAvatar(id, arquivo.getBytes(), UUID.fromString(jwt.getSubject()));
    }

    @DeleteMapping("/{id}/avatar")
    @Operation(summary = "Remove a foto de um usuário (somente ADMIN)")
    @ApiResponse(responseCode = "200", description = "Foto removida")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = USUARIO_NAO_ENCONTRADO)))
    public UsuarioDTO removerAvatar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return usuarioService.removerAvatar(id, UUID.fromString(jwt.getSubject()));
    }
}
