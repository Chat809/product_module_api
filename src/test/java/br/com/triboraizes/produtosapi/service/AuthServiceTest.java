package br.com.triboraizes.produtosapi.service;

import br.com.triboraizes.produtosapi.client.SupabaseAuthClient;
import br.com.triboraizes.produtosapi.client.SupabaseAuthClient.SessaoAuth;
import br.com.triboraizes.produtosapi.client.SupabaseAuthClient.UsuarioAuth;
import br.com.triboraizes.produtosapi.dto.GoogleLoginRequestDTO;
import br.com.triboraizes.produtosapi.dto.LoginRequestDTO;
import br.com.triboraizes.produtosapi.dto.RegistroRequestDTO;
import br.com.triboraizes.produtosapi.dto.SessaoDTO;
import br.com.triboraizes.produtosapi.model.Papel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final UUID ID = UUID.fromString("6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e");

    @Mock
    private SupabaseAuthClient authClient;

    @InjectMocks
    private AuthService authService;

    private static SessaoAuth sessao(Map<String, Object> app, Map<String, Object> dados) {
        return new SessaoAuth("acesso", "renovacao", 1793707200L, new UsuarioAuth(ID, "maria@exemplo.com", app, dados));
    }

    @Test
    @DisplayName("Registro deve normalizar o e-mail, criar a conta e já abrir a sessão")
    void deveRegistrarEEntrar() {
        when(authClient.entrar("maria@exemplo.com", "segredo1"))
                .thenReturn(sessao(Map.of("papel", "USUARIO", "acessos", List.of("dashboard", "produtos")),
                        Map.of("nome", "Maria")));

        SessaoDTO sessao = authService.registrar(new RegistroRequestDTO("  Maria ", " Maria@Exemplo.com ", "segredo1"));

        var ordem = inOrder(authClient);
        ordem.verify(authClient).criarUsuario("Maria", "maria@exemplo.com", "segredo1");
        ordem.verify(authClient).entrar("maria@exemplo.com", "segredo1");
        assertThat(sessao.usuario().papel()).isEqualTo(Papel.USUARIO);
        assertThat(sessao.usuario().nome()).isEqualTo("Maria");
        assertThat(sessao.usuario().acessos()).containsExactly("dashboard", "produtos");
    }

    @Test
    @DisplayName("Login deve montar a sessão com o papel do app_metadata e a expiração")
    void deveEntrar() {
        when(authClient.entrar("maria@exemplo.com", "segredo1"))
                .thenReturn(sessao(Map.of("papel", "ADMIN"), Map.of("nome", "Maria")));

        SessaoDTO sessao = authService.entrar(new LoginRequestDTO("MARIA@exemplo.com", "segredo1"));

        assertThat(sessao.accessToken()).isEqualTo("acesso");
        assertThat(sessao.refreshToken()).isEqualTo("renovacao");
        assertThat(sessao.expiraEm()).isEqualTo(Instant.ofEpochSecond(1793707200L));
        assertThat(sessao.usuario().id()).isEqualTo(ID);
        assertThat(sessao.usuario().papel()).isEqualTo(Papel.ADMIN);
        assertThat(sessao.usuario().acessos()).containsExactly("*");
    }

    @Test
    @DisplayName("Login com Google deve trocar o código e usar o nome do perfil do Google")
    void deveEntrarComGoogle() {
        String verificador = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
        when(authClient.trocarCodigo("codigo-google", verificador))
                .thenReturn(sessao(Map.of("provider", "google", "papel", "USUARIO", "acessos", List.of("dashboard")),
                        Map.of("full_name", "Maria Souza", "name", "Maria S.")));

        SessaoDTO sessao = authService.entrarComGoogle(new GoogleLoginRequestDTO("codigo-google", verificador));

        assertThat(sessao.accessToken()).isEqualTo("acesso");
        assertThat(sessao.usuario().nome()).isEqualTo("Maria Souza");
        assertThat(sessao.usuario().acessos()).containsExactly("dashboard");
    }

    @Test
    @DisplayName("Conta com senha vinculada ao Google mantém o nome do cadastro e o papel de admin")
    void deveManterContaExistenteNoLoginComGoogle() {
        when(authClient.trocarCodigo("codigo", "v".repeat(43)))
                .thenReturn(sessao(Map.of("papel", "ADMIN", "acessos", List.of("*")),
                        Map.of("nome", "Maria", "full_name", "Maria Souza", "name", " ")));

        SessaoDTO sessao = authService.entrarComGoogle(new GoogleLoginRequestDTO("codigo", "v".repeat(43)));

        assertThat(sessao.usuario().nome()).isEqualTo("Maria");
        assertThat(sessao.usuario().papel()).isEqualTo(Papel.ADMIN);
        assertThat(sessao.usuario().acessos()).containsExactly("*");
    }

    @Test
    @DisplayName("Sem metadados, o nome cai para o e-mail e o papel para USUARIO")
    void deveUsarPadroesSemMetadados() {
        when(authClient.renovar("renovacao")).thenReturn(sessao(null, null));

        SessaoDTO sessao = authService.renovar("renovacao");

        assertThat(sessao.usuario().nome()).isEqualTo("maria@exemplo.com");
        assertThat(sessao.usuario().papel()).isEqualTo(Papel.USUARIO);
        assertThat(sessao.usuario().acessos()).containsExactly("dashboard", "produtos", "demandas");
    }
}
