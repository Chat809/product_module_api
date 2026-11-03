package br.com.triboraizes.produtosapi.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FormatoDeImagemTest {

    @Test
    @DisplayName("Deve reconhecer PNG, JPEG e WEBP pela assinatura")
    void deveReconhecerFormatos() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0};
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        byte[] webp = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1);

        assertThat(FormatoDeImagem.detectar(png)).contains(FormatoDeImagem.PNG);
        assertThat(FormatoDeImagem.detectar(jpeg)).contains(FormatoDeImagem.JPEG);
        assertThat(FormatoDeImagem.detectar(webp)).contains(FormatoDeImagem.WEBP);
        assertThat(FormatoDeImagem.WEBP.tipo()).isEqualTo("image/webp");
        assertThat(FormatoDeImagem.JPEG.extensao()).isEqualTo("jpg");
    }

    @Test
    @DisplayName("Deve recusar outros arquivos, mesmo com nome ou tipo de imagem")
    void deveRecusarOutrosArquivos() {
        assertThat(FormatoDeImagem.detectar("<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(FormatoDeImagem.detectar("RIFF\0\0\0\0WAVE".getBytes(StandardCharsets.ISO_8859_1))).isEmpty();
        assertThat(FormatoDeImagem.detectar(new byte[]{(byte) 0xFF})).isEmpty();
        assertThat(FormatoDeImagem.detectar(null)).isEmpty();
    }
}
