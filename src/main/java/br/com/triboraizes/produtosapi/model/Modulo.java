package br.com.triboraizes.produtosapi.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Módulos do painel. Cada um é liberado pela coluna {@code acessos} do perfil, gravada com a {@link #chave()}. */
public enum Modulo {
    DASHBOARD,
    PRODUTOS,
    DEMANDAS,
    FINANCEIRO,
    USUARIOS;

    public String chave() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Autoridade do Spring Security que o converter do token concede para este módulo. */
    public String autoridade() {
        return "MODULO_" + name();
    }

    public static Optional<Modulo> de(String chave) {
        return Arrays.stream(values()).filter(modulo -> modulo.chave().equals(chave)).findFirst();
    }
}
