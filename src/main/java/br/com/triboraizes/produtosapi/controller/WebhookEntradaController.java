package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.dto.LeadRecebidoDTO;
import br.com.triboraizes.produtosapi.exception.CargaGrandeDemaisException;
import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import br.com.triboraizes.produtosapi.model.Lead;
import br.com.triboraizes.produtosapi.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/webhooks/entrada")
@Tag(name = "Webhook de entrada", description = "Endereço público que outros sistemas (formulário do site, "
        + "automações) usam para criar leads. O token na URL identifica a integração; corpo JSON de até 16 KB.")
public class WebhookEntradaController {

    private static final int LIMITE_DO_CORPO = 16 * 1024;

    private final WebhookService service;
    private final JsonMapper json;
    private final Validator validator;

    public WebhookEntradaController(WebhookService service, JsonMapper json, Validator validator) {
        this.service = service;
        this.json = json;
        this.validator = validator;
    }

    @PostMapping(value = "/{token}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Recebe um lead", description = "Campos: nome (obrigatório), empresa, email, telefone, "
            + "valorEstimado e mensagem. Responde 202 com o id do lead; 404 se o token não existir ou estiver inativo.")
    public Map<String, Object> receber(@PathVariable String token, HttpServletRequest requisicao) throws IOException {
        if (requisicao.getContentLengthLong() > LIMITE_DO_CORPO) {
            throw new CargaGrandeDemaisException("O corpo deve ter no máximo 16 KB");
        }
        byte[] corpo;
        try (InputStream entrada = requisicao.getInputStream()) {
            corpo = entrada.readNBytes(LIMITE_DO_CORPO + 1);
        }
        if (corpo.length > LIMITE_DO_CORPO) {
            throw new CargaGrandeDemaisException("O corpo deve ter no máximo 16 KB");
        }
        LeadRecebidoDTO recebido;
        try {
            recebido = json.readValue(corpo, LeadRecebidoDTO.class);
        } catch (JacksonException ex) {
            throw new RequisicaoInvalidaException("JSON inválido");
        }
        if (recebido == null) {
            throw new RequisicaoInvalidaException("Envie os dados do lead");
        }
        Set<ConstraintViolation<LeadRecebidoDTO>> erros = validator.validate(recebido);
        if (!erros.isEmpty()) {
            throw new RequisicaoInvalidaException(erros.stream().map(ConstraintViolation::getMessage).sorted()
                    .collect(Collectors.joining("; ")));
        }
        Lead lead = service.receberLead(token, recebido);
        return Map.of("leadId", lead.id());
    }
}
