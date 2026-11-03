package br.com.triboraizes.produtosapi.model;

import br.com.triboraizes.produtosapi.exception.RequisicaoInvalidaException;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Regras da coluna {@code acessos} (jsonb) do perfil: uma lista de chaves de {@link Modulo} ou {@code ["*"]}.
 * Administradores têm sempre acesso total, e o módulo de usuários é exclusivo deles.
 */
public final class Acessos {

    public static final String TOTAL = "*";
    public static final List<String> ACESSO_TOTAL = List.of(TOTAL);
    /** O mesmo default da coluna no banco, aplicado a toda conta nova. */
    public static final List<String> PADRAO =
            List.of(Modulo.DASHBOARD.chave(), Modulo.PRODUTOS.chave(), Modulo.DEMANDAS.chave());

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

    /** Módulos efetivamente liberados. Para usuários comuns, {@code *} libera tudo exceto {@link Modulo#USUARIOS}. */
    public static Set<Modulo> liberados(Papel papel, Collection<String> acessos) {
        if (papel == Papel.ADMIN) {
            return EnumSet.allOf(Modulo.class);
        }
        EnumSet<Modulo> modulos = EnumSet.noneOf(Modulo.class);
        if (acessos.contains(TOTAL)) {
            modulos.addAll(EnumSet.allOf(Modulo.class));
        } else {
            acessos.forEach(chave -> Modulo.de(chave).ifPresent(modulos::add));
        }
        modulos.remove(Modulo.USUARIOS);
        return modulos;
    }

    /** Valida os acessos pedidos para gravação e devolve a forma canônica: {@code ["*"]} ou módulos na ordem do enum. */
    public static List<String> normalizar(Papel papel, Collection<String> acessos) {
        if (papel == Papel.ADMIN) {
            return ACESSO_TOTAL;
        }
        EnumSet<Modulo> modulos = EnumSet.noneOf(Modulo.class);
        for (String chave : acessos) {
            if (TOTAL.equals(chave)) {
                return ACESSO_TOTAL;
            }
            Modulo modulo = Modulo.de(chave).orElseThrow(() -> new RequisicaoInvalidaException(
                    "Acesso desconhecido: '" + chave + "'. Use '*', 'dashboard', 'produtos', 'demandas' ou 'financeiro'"));
            if (modulo == Modulo.USUARIOS) {
                throw new RequisicaoInvalidaException("O módulo 'usuarios' é exclusivo de administradores");
            }
            modulos.add(modulo);
        }
        return modulos.stream().map(Modulo::chave).toList();
    }
}
