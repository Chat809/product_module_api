package br.com.triboraizes.produtosapi.model;

import java.util.Arrays;
import java.util.Optional;

/**
 * Subabas do painel, na mesma ordem do menu. A chave ({@code modulo/subaba}) é o que fica gravado na coluna
 * {@code acessos} do perfil. Subabas {@code somenteAdmin} nunca são liberadas para usuários comuns; as
 * {@code livres} valem para qualquer pessoa logada.
 */
public enum Submodulo {
    INICIO_DASHBOARD(Modulo.INICIO, "dashboard", false, false),
    ESTOQUE_PRODUTOS(Modulo.ESTOQUE, "produtos", false, false),
    DEMANDAS_QUADRO(Modulo.DEMANDAS, "quadro", false, false),
    FINANCEIRO_CONTAS_PAGAR(Modulo.FINANCEIRO, "contas-pagar", false, false),
    FINANCEIRO_CONTAS_RECEBER(Modulo.FINANCEIRO, "contas-receber", false, false),
    FINANCEIRO_FORNECEDORES(Modulo.FINANCEIRO, "fornecedores", false, false),
    FINANCEIRO_CENTROS_CUSTO(Modulo.FINANCEIRO, "centros-custo", false, false),
    CRM_CLIENTES(Modulo.CRM, "clientes", false, false),
    CRM_LEADS(Modulo.CRM, "leads", false, false),
    CRM_RECLAMACOES(Modulo.CRM, "reclamacoes", false, false),
    RELATORIOS_FINANCEIRO(Modulo.RELATORIOS, "financeiro", false, false),
    RELATORIOS_COMERCIAL(Modulo.RELATORIOS, "comercial", false, false),
    RELATORIOS_ESTOQUE(Modulo.RELATORIOS, "estoque", false, false),
    RELATORIOS_ATENDIMENTO(Modulo.RELATORIOS, "atendimento", false, false),
    CONFIGURACOES_USUARIOS(Modulo.CONFIGURACOES, "usuarios", true, false),
    CONFIGURACOES_PARAMETROS_CRM(Modulo.CONFIGURACOES, "parametros-crm", true, false),
    CONFIGURACOES_NOTIFICACOES(Modulo.CONFIGURACOES, "notificacoes", false, true),
    CONFIGURACOES_WEBHOOKS(Modulo.CONFIGURACOES, "webhooks", true, false),
    CONFIGURACOES_INTEGRACOES(Modulo.CONFIGURACOES, "integracoes", true, false);

    private final Modulo modulo;
    private final String subaba;
    private final boolean somenteAdmin;
    private final boolean livre;

    Submodulo(Modulo modulo, String subaba, boolean somenteAdmin, boolean livre) {
        this.modulo = modulo;
        this.subaba = subaba;
        this.somenteAdmin = somenteAdmin;
        this.livre = livre;
    }

    public Modulo modulo() {
        return modulo;
    }

    public boolean somenteAdmin() {
        return somenteAdmin;
    }

    public boolean livre() {
        return livre;
    }

    public String chave() {
        return modulo.chave() + "/" + subaba;
    }

    public String autoridade() {
        return "SUB_" + name();
    }

    public static Optional<Submodulo> de(String chave) {
        return Arrays.stream(values()).filter(sub -> sub.chave().equals(chave)).findFirst();
    }
}
