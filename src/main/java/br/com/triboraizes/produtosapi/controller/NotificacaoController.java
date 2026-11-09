package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.PaginaDTO;
import br.com.triboraizes.produtosapi.dto.PreferenciasNotificacaoDTO;
import br.com.triboraizes.produtosapi.model.Notificacao;
import br.com.triboraizes.produtosapi.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notificacoes")
@Tag(name = "Notificações", description = "Notificações e preferências do usuário autenticado.")
public class NotificacaoController {

    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Minhas notificações, das mais recentes para as mais antigas")
    public PaginaDTO<Notificacao> listar(@RequestParam(defaultValue = "false") boolean naoLidas,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         Authentication autenticacao) {
        return service.listar(usuario(autenticacao), naoLidas, page, size);
    }

    @GetMapping("/nao-lidas")
    @Operation(summary = "Quantas notificações não lidas eu tenho (consultado periodicamente pelo sino)")
    public Map<String, Long> naoLidas(Authentication autenticacao) {
        return Map.of("total", service.naoLidas(usuario(autenticacao)));
    }

    @PatchMapping("/{id}/lida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Marca uma notificação como lida")
    public void marcarLida(@PathVariable UUID id, Authentication autenticacao) {
        service.marcarLida(id, usuario(autenticacao));
    }

    @PatchMapping("/lidas")
    @Operation(summary = "Marca todas as minhas notificações como lidas")
    public Map<String, Integer> marcarTodasLidas(Authentication autenticacao) {
        return Map.of("marcadas", service.marcarTodasLidas(usuario(autenticacao)));
    }

    @GetMapping("/preferencias")
    @Operation(summary = "Minhas preferências de notificação")
    public PreferenciasNotificacaoDTO preferencias(Authentication autenticacao) {
        return service.preferencias(usuario(autenticacao));
    }

    @PutMapping("/preferencias")
    @Operation(summary = "Salva minhas preferências de notificação")
    public PreferenciasNotificacaoDTO salvarPreferencias(@Valid @RequestBody PreferenciasNotificacaoDTO request,
                                                         Authentication autenticacao) {
        return service.salvarPreferencias(usuario(autenticacao), request.preferencias());
    }

    private static UUID usuario(Authentication autenticacao) {
        return UUID.fromString(autenticacao.getName());
    }
}
