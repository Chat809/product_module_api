package br.com.triboraizes.produtosapi;

import br.com.triboraizes.produtosapi.service.ProdutoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProdutosApiApplicationTests {

    @Autowired
    private ProdutoService produtoService;

    @Test
    @DisplayName("Deve subir a aplicação com os produtos de exemplo carregados")
    void deveSubirComDadosIniciais() {
        assertThat(produtoService.resumo().totalProdutos()).isEqualTo(15);
        assertThat(produtoService.resumo().esgotados()).isEqualTo(2);
    }
}
