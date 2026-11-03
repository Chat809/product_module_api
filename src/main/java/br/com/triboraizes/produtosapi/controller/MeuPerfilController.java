package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.AtualizarMeuPerfilRequestDTO;
import br.com.triboraizes.produtosapi.dto.MeuPerfilDTO;
import br.com.triboraizes.produtosapi.service.MeuPerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.ARQUIVO_GRANDE;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.DADOS_INDISPONIVEIS;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.DADOS_INVALIDOS;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.IMAGEM_INVALIDA;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.NAO_AUTENTICADO;
import static br.com.triboraizes.produtosapi.controller.ExemplosDeErro.PROBLEM_JSON;

@RestController
@RequestMapping("/api/v1/usuarios/me")
@Tag(name = "Meu perfil", description = "Dados e avatar do usuário logado. Qualquer usuário autenticado acessa o próprio perfil.")
@ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
        content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                examples = @ExampleObject(value = NAO_AUTENTICADO)))
public class MeuPerfilController {

    private final MeuPerfilService meuPerfilService;

    public MeuPerfilController(MeuPerfilService meuPerfilService) {
        this.meuPerfilService = meuPerfilService;
    }

    @GetMapping
    @Operation(summary = "Devolve o perfil completo do usuário logado, com papel e acessos atuais do banco")
    @ApiResponse(responseCode = "200", description = "Perfil do usuário logado")
    public MeuPerfilDTO buscar(@AuthenticationPrincipal Jwt jwt) {
        return meuPerfilService.buscar(usuario(jwt));
    }

    @PutMapping
    @Operation(summary = "Edita nome, telefone e bio do próprio usuário",
            description = "E-mail, nível de acesso, acessos, departamento e cargo não são editáveis aqui: "
                    + "só um administrador os altera.")
    @ApiResponse(responseCode = "200", description = "Perfil atualizado")
    @ApiResponse(responseCode = "400", description = "Campos inválidos",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = DADOS_INVALIDOS)))
    public MeuPerfilDTO atualizar(@Valid @RequestBody AtualizarMeuPerfilRequestDTO request,
                                  @AuthenticationPrincipal Jwt jwt) {
        return meuPerfilService.atualizar(usuario(jwt), request);
    }

    @PutMapping(path = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Envia ou troca o avatar (PNG, JPEG ou WEBP, até 2 MB)",
            description = "O formato é conferido pelo conteúdo do arquivo. A imagem anterior é apagada do Storage.")
    @ApiResponse(responseCode = "200", description = "Avatar atualizado; a URL pública vem em avatarUrl")
    @ApiResponse(responseCode = "400", description = "Arquivo ausente ou formato não suportado",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = IMAGEM_INVALIDA)))
    @ApiResponse(responseCode = "413", description = "Arquivo maior que 2 MB",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = ARQUIVO_GRANDE)))
    @ApiResponse(responseCode = "503", description = "Storage indisponível",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = DADOS_INDISPONIVEIS)))
    public MeuPerfilDTO trocarAvatar(@Parameter(description = "Imagem do avatar") @RequestPart("arquivo") MultipartFile arquivo,
                                     @AuthenticationPrincipal Jwt jwt) throws IOException {
        return meuPerfilService.trocarAvatar(usuario(jwt), arquivo.getBytes());
    }

    @DeleteMapping("/avatar")
    @Operation(summary = "Remove o avatar; o painel volta a mostrar as iniciais")
    @ApiResponse(responseCode = "200", description = "Avatar removido")
    public MeuPerfilDTO removerAvatar(@AuthenticationPrincipal Jwt jwt) {
        return meuPerfilService.removerAvatar(usuario(jwt));
    }

    private static UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
