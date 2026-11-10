package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.WebhookEntradaDTO;
import br.com.triboraizes.produtosapi.dto.WebhookEntradaRequestDTO;
import br.com.triboraizes.produtosapi.dto.WebhookSaidaDTO;
import br.com.triboraizes.produtosapi.dto.WebhookSaidaRequestDTO;
import br.com.triboraizes.produtosapi.evento.EventoDeNegocio;
import br.com.triboraizes.produtosapi.model.WebhookEntrega;
import br.com.triboraizes.produtosapi.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

@RestController
@RequestMapping("/api/v1/configuracoes/webhooks")
@Tag(name = "Webhooks", description = "Configurações > Webhooks. Saída: avisa outros sistemas sobre eventos, com "
        + "assinatura HMAC-SHA256 no cabeçalho X-Tribo-Assinatura. Entrada: endereço que recebe leads. Segredos e "
        + "tokens só aparecem completos ao criar ou regenerar.")
public class WebhookConfigController {

    private final WebhookService service;

    public WebhookConfigController(WebhookService service) {
        this.service = service;
    }

    @GetMapping("/eventos")
    @Operation(summary = "Eventos que podem ser assinados")
    public List<String> eventos() {
        return EventoDeNegocio.TIPOS;
    }

    @GetMapping("/saida")
    @Operation(summary = "Webhooks de saída (segredo mascarado)")
    public List<WebhookSaidaDTO> saidas() {
        return service.saidas();
    }

    @PostMapping("/saida")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um webhook de saída; a resposta traz o segredo completo")
    public WebhookSaidaDTO criarSaida(@Valid @RequestBody WebhookSaidaRequestDTO request,
                                      @AuthenticationPrincipal Jwt jwt) {
        return service.criarSaida(request, UUID.fromString(jwt.getSubject()));
    }

    @PutMapping("/saida/{id}")
    @Operation(summary = "Atualiza um webhook de saída")
    public WebhookSaidaDTO atualizarSaida(@PathVariable UUID id, @Valid @RequestBody WebhookSaidaRequestDTO request) {
        return service.atualizarSaida(id, request);
    }

    @PostMapping("/saida/{id}/segredo")
    @Operation(summary = "Gera um novo segredo; o anterior deixa de valer na hora")
    public WebhookSaidaDTO regenerarSegredo(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.regenerarSegredo(id, UUID.fromString(jwt.getSubject()));
    }

    @PostMapping("/saida/{id}/teste")
    @Operation(summary = "Envia um evento de teste e devolve o resultado da entrega")
    public WebhookEntrega testar(@PathVariable UUID id) {
        return service.testar(id);
    }

    @DeleteMapping("/saida/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um webhook de saída e o log das suas entregas")
    public void excluirSaida(@PathVariable UUID id) {
        service.excluirSaida(id);
    }

    @GetMapping("/entregas")
    @Operation(summary = "Log de entregas (30 dias), das mais recentes para as mais antigas")
    public PaginaDTO<WebhookEntrega> entregas(@RequestParam(required = false) UUID webhookId,
                                              @RequestParam(required = false) Boolean sucesso,
                                              @RequestParam(defaultValue = "0") int pagina,
                                              @RequestParam(defaultValue = "20") int tamanho) {
        return service.entregas(webhookId, sucesso, pagina, tamanho);
    }

    @GetMapping("/entrada")
    @Operation(summary = "Webhooks de entrada (token mascarado)")
    public List<WebhookEntradaDTO> entradas() {
        return service.entradas();
    }

    @PostMapping("/entrada")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um webhook de entrada; a resposta traz o token completo")
    public WebhookEntradaDTO criarEntrada(@Valid @RequestBody WebhookEntradaRequestDTO request,
                                          @AuthenticationPrincipal Jwt jwt) {
        return service.criarEntrada(request, UUID.fromString(jwt.getSubject()));
    }

    @PutMapping("/entrada/{id}")
    @Operation(summary = "Atualiza um webhook de entrada")
    public WebhookEntradaDTO atualizarEntrada(@PathVariable UUID id,
                                              @Valid @RequestBody WebhookEntradaRequestDTO request) {
        return service.atualizarEntrada(id, request);
    }

    @PostMapping("/entrada/{id}/token")
    @Operation(summary = "Gera um novo token (muda o endereço); o anterior deixa de valer na hora")
    public WebhookEntradaDTO regenerarToken(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return service.regenerarToken(id, UUID.fromString(jwt.getSubject()));
    }

    @DeleteMapping("/entrada/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um webhook de entrada")
    public void excluirEntrada(@PathVariable UUID id) {
        service.excluirEntrada(id);
    }
}
