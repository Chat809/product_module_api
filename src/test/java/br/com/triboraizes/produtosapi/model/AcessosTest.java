package br.com.triboraizes.produtosapi.model;

import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AcessosTest {

    @Test
    @DisplayName("ADMIN sempre grava acesso total, qualquer que seja o pedido")
    void adminSempreTemAcessoTotal() {
        assertThat(Acessos.normalizar(Papel.ADMIN, List.of("dashboard"))).containsExactly("*");
    }

    @Test
    @DisplayName("Deve remover duplicados e ordenar os módulos pela ordem do menu")
    void deveNormalizarLista() {
        assertThat(Acessos.normalizar(Papel.USUARIO, List.of("financeiro", "produtos", "dashboard", "produtos", "demandas")))
                .containsExactly("dashboard", "produtos", "demandas", "financeiro");
        assertThat(Acessos.normalizar(Papel.USUARIO, List.of())).isEmpty();
    }

    @Test
    @DisplayName("'*' prevalece sobre módulos individuais")
    void asteriscoPrevalece() {
        assertThat(Acessos.normalizar(Papel.USUARIO, List.of("dashboard", "*"))).containsExactly("*");
    }

    @Test
    @DisplayName("Deve recusar acesso desconhecido e o módulo de usuários para USUARIO")
    void deveRecusarAcessosInvalidos() {
        assertThatThrownBy(() -> Acessos.normalizar(Papel.USUARIO, List.of("relatorios")))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessageContaining("relatorios");
        assertThatThrownBy(() -> Acessos.normalizar(Papel.USUARIO, List.of("usuarios")))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessageContaining("exclusivo de administradores");
    }

    @Test
    @DisplayName("Deve ler os acessos do token, com padrão quando ausentes")
    void deveLerDoToken() {
        assertThat(Acessos.de(List.of("dashboard"), Papel.USUARIO)).containsExactly("dashboard");
        assertThat(Acessos.de(null, Papel.USUARIO)).containsExactly("dashboard", "produtos", "demandas");
        assertThat(Acessos.de(List.of("dashboard"), Papel.ADMIN)).containsExactly("*");
    }
}
