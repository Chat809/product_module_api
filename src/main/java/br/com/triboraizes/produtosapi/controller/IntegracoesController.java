package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.config.SupabaseProperties;
import br.com.triboraizes.produtosapi.dto.WebhookEntradaDTO;
import br.com.triboraizes.produtosapi.dto.WebhookSaidaDTO;
import br.com.triboraizes.produtosapi.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/configuracoes/integracoes")
@Tag(name = "Integrações", description = "Situação dos serviços externos. Nunca devolve chaves ou segredos.")
public class IntegracoesController {

    public record Integracao(String nome, boolean ativa, String detalhe) {
    }

    private final SupabaseProperties supabase;
    private final RestClient supabaseRestClient;
    private final WebhookService webhooks;
    private final boolean emailHabilitado;

    public IntegracoesController(SupabaseProperties supabase,
                                 @Qualifier("supabaseRestClient") RestClient supabaseRestClient,
                                 WebhookService webhooks,
                                 @Value("${app.email.habilitado:false}") boolean emailHabilitado) {
        this.supabase = supabase;
        this.supabaseRestClient = supabaseRestClient;
        this.webhooks = webhooks;
        this.emailHabilitado = emailHabilitado;
    }

    @GetMapping
    @Operation(summary = "Supabase, login com Google, e-mail e webhooks")
    public List<Integracao> listar() {
        List<WebhookSaidaDTO> saidas = webhooks.saidas();
        List<WebhookEntradaDTO> entradas = webhooks.entradas();
        long saidasAtivas = saidas.stream().filter(WebhookSaidaDTO::ativo).count();
        long entradasAtivas = entradas.stream().filter(WebhookEntradaDTO::ativo).count();
        return List.of(
                new Integracao("Supabase", supabase.configurado(),
                        supabase.configurado() ? "Banco, autenticação e arquivos conectados"
                                : "Chave do servidor não configurada"),
                loginGoogle(),
                new Integracao("E-mail", emailHabilitado, emailHabilitado ? "Envio de notificações por e-mail ligado"
                        : "Sem provedor de e-mail configurado; as notificações ficam só no painel"),
                new Integracao("Webhooks", saidasAtivas + entradasAtivas > 0,
                        saidasAtivas + " de saída e " + entradasAtivas + " de entrada ativos"));
    }

    @SuppressWarnings("unchecked")
    private Integracao loginGoogle() {
        if (!supabase.configurado()) {
            return new Integracao("Login com Google", false, "Supabase não configurado");
        }
        try {
            Map<String, Object> config = supabaseRestClient.get().uri("/auth/v1/settings").retrieve().body(Map.class);
            Object externos = config == null ? null : config.get("external");
            boolean ativo = externos instanceof Map<?, ?> mapa && Boolean.TRUE.equals(mapa.get("google"));
            return new Integracao("Login com Google", ativo,
                    ativo ? "Provedor Google habilitado no Supabase" : "Provedor Google desabilitado no Supabase");
        } catch (RuntimeException ex) {
            return new Integracao("Login com Google", false, "Não foi possível consultar o Supabase");
        }
    }
}
