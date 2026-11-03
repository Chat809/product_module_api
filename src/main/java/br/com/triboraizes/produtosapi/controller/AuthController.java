package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.LoginRequestDTO;
import br.com.triboraizes.produtosapi.dto.RefreshRequestDTO;
import br.com.triboraizes.produtosapi.dto.RegistroRequestDTO;
import br.com.triboraizes.produtosapi.dto.SessaoDTO;
import br.com.triboraizes.produtosapi.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.CREDENCIAIS_INVALIDAS;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.DADOS_INVALIDOS;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.EMAIL_JA_CADASTRADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.PROBLEM_JSON;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.SERVICO_INDISPONIVEL;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Registro, login e renovação de sessão (rotas públicas)")
@SecurityRequirements
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma conta de usuário e já abre a sessão",
            description = "A conta entra com o papel USUARIO. Promoções a ADMIN são feitas por um administrador.")
    @ApiResponse(responseCode = "201", description = "Conta criada e sessão aberta")
    @ApiResponse(responseCode = "400", description = "Campos inválidos",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = DADOS_INVALIDOS)))
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = EMAIL_JA_CADASTRADO)))
    @ApiResponse(responseCode = "503", description = "Supabase indisponível ou não configurado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = SERVICO_INDISPONIVEL)))
    public SessaoDTO registrar(@Valid @RequestBody RegistroRequestDTO request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Abre uma sessão com e-mail e senha")
    @ApiResponse(responseCode = "200", description = "Sessão aberta")
    @ApiResponse(responseCode = "400", description = "Campos inválidos",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = DADOS_INVALIDOS)))
    @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = CREDENCIAIS_INVALIDAS)))
    @ApiResponse(responseCode = "503", description = "Supabase indisponível ou não configurado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = SERVICO_INDISPONIVEL)))
    public SessaoDTO entrar(@Valid @RequestBody LoginRequestDTO request) {
        return authService.entrar(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Troca o refreshToken por uma sessão nova",
            description = "Também atualiza o papel no token depois que um administrador o altera.")
    @ApiResponse(responseCode = "200", description = "Sessão renovada")
    @ApiResponse(responseCode = "401", description = "refreshToken inválido ou já usado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public SessaoDTO renovar(@Valid @RequestBody RefreshRequestDTO request) {
        return authService.renovar(request.refreshToken());
    }
}
