package br.com.triboraizes.produtosapi;

import br.com.triboraizes.produtosapi.exception.ServicoDeDadosIndisponivelException;
import br.com.triboraizes.produtosapi.service.ProdutoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Sobe o contexto inteiro sem banco local: os dados ficam no Supabase, que exige a chave secreta. */
@SpringBootTest(properties = "app.supabase.secret-key=")
class ProdutosApiApplicationTests {

    @Autowired
    private ProdutoService produtoService;

    @Test
    @DisplayName("Deve subir a aplicação sem H2 e avisar com clareza quando a chave do Supabase faltar")
    void deveSubirSemBancoLocal() {
        assertThatThrownBy(() -> produtoService.resumo())
                .isInstanceOf(ServicoDeDadosIndisponivelException.class)
                .hasMessageContaining("SUPABASE_SECRET_KEY");
    }
}
