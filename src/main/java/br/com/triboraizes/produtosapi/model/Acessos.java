package br.com.triboraizes.produtosapi.model;

import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Regras da coluna {@code acessos} (jsonb) do perfil: {@code ["*"]} ou uma lista de chaves de
 * {@link Submodulo} ({@code financeiro/contas-pagar}). A chave de um {@link Modulo} libera todas as subabas dele,
 * e as chaves anteriores à divisão em subabas ({@code dashboard}, {@code produtos}) continuam valendo.
 * Administradores têm sempre acesso total.
 */
public final class Acessos {

    public static final String TOTAL = "*";
    public static final List<String> ACESSO_TOTAL = List.of(TOTAL);
    /** O mesmo default da coluna no banco, aplicado a toda conta nova. */
    public static final List<String> PADRAO = List.of(
            Submodulo.INICIO_DASHBOARD.chave(), Submodulo.ESTOQUE_PRODUTOS.chave(), Submodulo.DEMANDAS_QUADRO.chave());

    private Acessos() {
    }

    /**
     * Lê os acessos vindos do {@code app_metadata} do token. Tokens emitidos antes da coluna existir
     * não trazem o campo e recebem o padrão do papel.
     */
    public static List<String> de(Object valor, Papel papel) {
        if (papel == Papel.ADMIN) {
            return ACESSO_TOTAL;
        }
        if (valor instanceof Collection<?> lista) {
            return lista.stream().map(String::valueOf).toList();
        }
        return PADRAO;
    }

    /** Subabas efetivamente liberadas. Para usuários comuns, {@code *} libera tudo exceto as subabas de administrador. */
    public static Set<Submodulo> liberados(Papel papel, Collection<String> acessos) {
        if (papel == Papel.ADMIN) {
            return EnumSet.allOf(Submodulo.class);
        }
        EnumSet<Submodulo> subabas = EnumSet.noneOf(Submodulo.class);
        if (acessos.contains(TOTAL)) {
            subabas.addAll(EnumSet.allOf(Submodulo.class));
        } else {
            acessos.forEach(chave -> subabas.addAll(expandir(chave)));
        }
        subabas.removeIf(Submodulo::somenteAdmin);
        Arrays.stream(Submodulo.values()).filter(Submodulo::livre).forEach(subabas::add);
        return subabas;
    }

    /** Módulos com ao menos uma subaba liberada. */
    public static Set<Modulo> modulos(Set<Submodulo> subabas) {
        EnumSet<Modulo> modulos = EnumSet.noneOf(Modulo.class);
        subabas.forEach(sub -> modulos.add(sub.modulo()));
        return modulos;
    }

    /**
     * Valida os acessos pedidos para gravação e devolve a forma canônica: {@code ["*"]} ou as chaves das subabas
     * na ordem do menu (chaves de módulo viram todas as suas subabas).
     */
    public static List<String> normalizar(Papel papel, Collection<String> acessos) {
        if (papel == Papel.ADMIN) {
            return ACESSO_TOTAL;
        }
        EnumSet<Submodulo> subabas = EnumSet.noneOf(Submodulo.class);
        for (String chave : acessos) {
            if (TOTAL.equals(chave)) {
                return ACESSO_TOTAL;
            }
            Set<Submodulo> pedidas = expandir(chave);
            if (pedidas.isEmpty()) {
                throw new RequisicaoInvalidaException("Acesso desconhecido: '" + chave
                        + "'. Use '*', a chave de um módulo (ex.: 'crm') ou de uma subaba (ex.: 'crm/leads')");
            }
            if (pedidas.stream().allMatch(Submodulo::somenteAdmin)) {
                throw new RequisicaoInvalidaException("O acesso '" + chave + "' é exclusivo de administradores");
            }
            subabas.addAll(pedidas);
        }
        return subabas.stream()
                .filter(sub -> !sub.somenteAdmin() && !sub.livre())
                .map(Submodulo::chave)
                .toList();
    }

    private static Set<Submodulo> expandir(String chave) {
        EnumSet<Submodulo> subabas = EnumSet.noneOf(Submodulo.class);
        Submodulo.de(chave).ifPresent(subabas::add);
        Modulo.de(chave).ifPresent(modulo -> Arrays.stream(Submodulo.values())
                .filter(sub -> sub.modulo() == modulo)
                .forEach(subabas::add));
        return subabas;
    }
}
