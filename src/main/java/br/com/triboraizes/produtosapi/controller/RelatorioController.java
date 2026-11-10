package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.model.Relatorio;
import br.com.triboraizes.produtosapi.model.TipoRelatorio;
import br.com.triboraizes.produtosapi.service.RelatorioService;
import br.com.triboraizes.produtosapi.service.RelatorioService.Periodo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/relatorios")
@Tag(name = "Relatórios", description = "financeiro, comercial, estoque e atendimento; cada um exige a subaba "
        + "correspondente em Relatórios. Sem datas, o período é o mês corrente. O de estoque é uma foto do momento.")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    @GetMapping("/{tipo}")
    @Operation(summary = "Gera o relatório (indicadores, gráficos e tabelas)")
    public Relatorio gerar(@Parameter(example = "financeiro") @PathVariable String tipo,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        TipoRelatorio relatorio = service.tipo(tipo);
        return service.gerar(relatorio, service.periodo(inicio, fim));
    }

    @GetMapping("/{tipo}/csv")
    @Operation(summary = "Exporta o relatório em CSV (separador ;, para Excel em português)")
    public ResponseEntity<byte[]> csv(@PathVariable String tipo,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        TipoRelatorio relatorio = service.tipo(tipo);
        Periodo periodo = service.periodo(inicio, fim);
        String conteudo = service.csv(relatorio, periodo, service.gerar(relatorio, periodo));
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(service.nomeDoArquivo(relatorio, periodo)).build().toString())
                .body(conteudo.getBytes(StandardCharsets.UTF_8));
    }
}
