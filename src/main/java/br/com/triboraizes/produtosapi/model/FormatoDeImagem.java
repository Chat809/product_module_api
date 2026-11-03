package br.com.triboraizes.produtosapi.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Formatos aceitos para o avatar, reconhecidos pelos primeiros bytes do arquivo (assinatura),
 * e não pelo nome ou pelo Content-Type, que o cliente pode declarar como quiser.
 */
public enum FormatoDeImagem {
    PNG("image/png", "png"),
    JPEG("image/jpeg", "jpg"),
    WEBP("image/webp", "webp");

    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] ASSINATURA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_MARCA = {'W', 'E', 'B', 'P'};

    private final String tipo;
    private final String extensao;

    FormatoDeImagem(String tipo, String extensao) {
        this.tipo = tipo;
        this.extensao = extensao;
    }

    public String tipo() {
        return tipo;
    }

    public String extensao() {
        return extensao;
    }

    public static Optional<FormatoDeImagem> detectar(byte[] conteudo) {
        if (comecaCom(conteudo, 0, ASSINATURA_PNG)) {
            return Optional.of(PNG);
        }
        if (comecaCom(conteudo, 0, ASSINATURA_JPEG)) {
            return Optional.of(JPEG);
        }
        if (comecaCom(conteudo, 0, RIFF) && comecaCom(conteudo, 8, WEBP_MARCA)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean comecaCom(byte[] conteudo, int inicio, byte[] assinatura) {
        return conteudo != null && conteudo.length >= inicio + assinatura.length
                && Arrays.equals(conteudo, inicio, inicio + assinatura.length, assinatura, 0, assinatura.length);
    }
}
