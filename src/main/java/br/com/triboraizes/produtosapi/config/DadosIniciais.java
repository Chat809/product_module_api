package br.com.triboraizes.produtosapi.config;

import br.com.triboraizes.produtosapi.dto.MovimentacaoRequestDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoRequestDTO;
import br.com.triboraizes.produtosapi.dto.ProdutoResponseDTO;
import br.com.triboraizes.produtosapi.model.TipoMovimentacao;
import br.com.triboraizes.produtosapi.service.MovimentacaoService;
import br.com.triboraizes.produtosapi.service.ProdutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Popula o banco em memória com produtos de exemplo, para a aplicação não iniciar vazia.
 * Pode ser desligado com {@code app.dados-iniciais.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "app.dados-iniciais.enabled", havingValue = "true")
public class DadosIniciais implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    private static final List<ProdutoRequestDTO> PRODUTOS = List.of(
            produto("Teclado mecânico", "Teclado ABNT2 com switches marrons e iluminação RGB", "349.90", 25),
            produto("Mouse sem fio", "Mouse óptico 1600 DPI com receptor USB", "89.90", 40),
            produto("Monitor 27\"", "Monitor IPS Full HD de 75 Hz", "1299.00", 8),
            produto("Headset gamer", "Headset com microfone e som surround 7.1", "279.90", 3),
            produto("Webcam Full HD", "Webcam 1080p com microfone embutido", "199.90", 0),
            produto("Cadeira ergonômica", "Cadeira com apoio lombar e braços reguláveis", "1499.00", 5),
            produto("Notebook 15\"", "Notebook com 16 GB de RAM e SSD de 512 GB", "4299.00", 12),
            produto("SSD 1 TB", "SSD NVMe PCIe 4.0", "459.90", 30),
            produto("Hub USB-C", "Hub 7 em 1 com HDMI, USB 3.0 e leitor de cartões", "159.90", 2),
            produto("Mousepad estendido", "Mousepad 90 x 40 cm com base antiderrapante", "69.90", 60),
            produto("Suporte para monitor", "Braço articulado para monitores de até 32\"", "229.90", 0),
            produto("Impressora multifuncional", "Impressora jato de tinta com Wi-Fi", "899.00", 4),
            produto("Roteador Wi-Fi 6", "Roteador dual band AX1800", "549.90", 15),
            produto("Caixa de som Bluetooth", "Caixa portátil à prova d'água", "249.90", 18),
            produto("Carregador portátil", "Power bank de 20.000 mAh com carga rápida", "149.90", 1));

    private final ProdutoService produtoService;
    private final MovimentacaoService movimentacaoService;

    public DadosIniciais(ProdutoService produtoService, MovimentacaoService movimentacaoService) {
        this.produtoService = produtoService;
        this.movimentacaoService = movimentacaoService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (produtoService.resumo().totalProdutos() > 0) {
            return;
        }
        List<ProdutoResponseDTO> criados = PRODUTOS.stream().map(produtoService::criar).toList();

        movimentacaoService.registrar(criados.get(0).id(),
                new MovimentacaoRequestDTO(TipoMovimentacao.SAIDA, 5, "Venda para cliente corporativo"));
        movimentacaoService.registrar(criados.get(0).id(),
                new MovimentacaoRequestDTO(TipoMovimentacao.ENTRADA, 10, "Reposição do fornecedor"));
        movimentacaoService.registrar(criados.get(1).id(),
                new MovimentacaoRequestDTO(TipoMovimentacao.SAIDA, 12, "Venda no balcão"));

        log.info("Dados iniciais carregados: {} produtos de exemplo", criados.size());
    }

    private static ProdutoRequestDTO produto(String nome, String descricao, String preco, int quantidade) {
        return new ProdutoRequestDTO(nome, descricao, new BigDecimal(preco), quantidade, null);
    }
}
