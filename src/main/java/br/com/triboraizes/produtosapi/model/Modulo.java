package br.com.triboraizes.produtosapi.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Módulos (grupos do menu) do painel. Cada um reúne {@link Submodulo subabas}; gravar a chave do módulo
 * na coluna {@code acessos} libera todas as subabas dele.
 */
public enum Modulo {
    INICIO,
    ESTOQUE,
    DEMANDAS,
    FINANCEIRO,
    CRM,
    RELATORIOS,
    CONFIGURACOES;

    /** Chaves gravadas antes da divisão em subabas, que continuam valendo nos perfis e tokens antigos. */
    private static final Map<String, Modulo> CHAVES_ANTIGAS = Map.of("dashboard", INICIO, "produtos", ESTOQUE);

    public String chave() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Autoridade concedida quando ao menos uma subaba do módulo está liberada. */
    public String autoridade() {
        return "MODULO_" + name();
    }

    public static Optional<Modulo> de(String chave) {
        if (CHAVES_ANTIGAS.containsKey(chave)) {
            return Optional.of(CHAVES_ANTIGAS.get(chave));
        }
        return Arrays.stream(values()).filter(modulo -> modulo.chave().equals(chave)).findFirst();
    }
}
