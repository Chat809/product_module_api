package br.com.triboraizes.produtosapi.model;

/** Papel do usuário no sistema. Gravado na tabela {@code perfis} do Supabase e copiado para o JWT. */
public enum Papel {
    ADMIN,
    USUARIO;

    /** Converte o valor vindo do Supabase; ausente ou desconhecido vira {@link #USUARIO}. */
    public static Papel de(Object valor) {
        return ADMIN.name().equals(valor) ? ADMIN : USUARIO;
    }
}
