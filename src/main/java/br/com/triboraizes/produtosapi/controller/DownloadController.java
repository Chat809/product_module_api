package br.com.triboraizes.produtosapi.controller;

import br.com.triboraizes.produtosapi.exception.RegistroNaoEncontradoException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Entrega, como anexo, o arquivo colocado em {@code src/main/resources/download/}. Arquivos ocultos (como o
 * {@code .gitkeep}) são ignorados; havendo mais de um, vale o primeiro em ordem alfabética.
 */
@RestController
@RequestMapping("/api/v1/download")
@Tag(name = "Download", description = "Download público do arquivo distribuído pela API.")
public class DownloadController {

    private static final String PADRAO = "classpath:download/*";

    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @GetMapping
    @Operation(summary = "Baixa o arquivo disponível para download")
    public ResponseEntity<Resource> baixar() throws IOException {
        Resource arquivo = Arrays.stream(resolver.getResources(PADRAO))
                .filter(Resource::isReadable)
                .filter(recurso -> recurso.getFilename() != null && !recurso.getFilename().startsWith("."))
                .min(Comparator.comparing(Resource::getFilename))
                .orElseThrow(() -> new RegistroNaoEncontradoException(
                        "Arquivo não encontrado", "Nenhum arquivo disponível para download."));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(arquivo.contentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(arquivo.getFilename(), StandardCharsets.UTF_8).build().toString())
                .body(arquivo);
    }
}
