package br.com.triboraizes.produtosapi.controller;

/** Exemplos de respostas de erro (RFC 9457) exibidos na documentação do Swagger. */
final class ExemplosDeErro {

    static final String PROBLEM_JSON = "application/problem+json";

    static final String DADOS_INVALIDOS = """
            {
              "title": "Dados inválidos",
              "status": 400,
              "detail": "Um ou mais campos são inválidos",
              "instance": "/api/v1/produtos",
              "erros": [
                {"campo": "nome", "mensagem": "O nome é obrigatório"},
                {"campo": "preco", "mensagem": "O preço deve ser maior que zero"}
              ]
            }""";

    static final String PARAMETRO_INVALIDO = """
            {
              "title": "Parâmetro inválido",
              "status": 400,
              "detail": "O parâmetro 'size' deve estar entre 1 e 500",
              "instance": "/api/v1/produtos"
            }""";

    static final String NAO_ENCONTRADO = """
            {
              "title": "Produto não encontrado",
              "status": 404,
              "detail": "Produto com id 3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10 não encontrado",
              "instance": "/api/v1/produtos/3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10"
            }""";

    static final String CONFLITO = """
            {
              "title": "Conflito de edição",
              "status": 409,
              "detail": "O produto 3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10 foi alterado por outra pessoa. Recarregue os dados e tente novamente.",
              "instance": "/api/v1/produtos/3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10"
            }""";

    static final String ESTOQUE_INSUFICIENTE = """
            {
              "title": "Estoque insuficiente",
              "status": 422,
              "detail": "Estoque insuficiente: há 12 unidade(s) disponível(is) e a saída pedida é de 999",
              "instance": "/api/v1/produtos/3f1c2b4e-8a6d-4e2f-9b1a-0c5d7e8f9a10/movimentacoes"
            }""";

    static final String NAO_AUTENTICADO = """
            {
              "title": "Não autenticado",
              "status": 401,
              "detail": "Faça login e envie o token no cabeçalho Authorization: Bearer <token>",
              "instance": "/api/v1/produtos"
            }""";

    static final String ACESSO_NEGADO = """
            {
              "title": "Acesso negado",
              "status": 403,
              "detail": "Seu usuário não tem permissão para esta operação. Peça a um administrador para liberar o acesso.",
              "instance": "/api/v1/usuarios"
            }""";

    static final String CREDENCIAIS_INVALIDAS = """
            {
              "title": "Credenciais inválidas",
              "status": 401,
              "detail": "E-mail ou senha inválidos",
              "instance": "/api/v1/auth/login"
            }""";

    static final String GOOGLE_EXPIRADO = """
            {
              "title": "Credenciais inválidas",
              "status": 401,
              "detail": "Login com Google expirado ou inválido. Tente novamente.",
              "instance": "/api/v1/auth/google"
            }""";

    static final String EMAIL_JA_CADASTRADO = """
            {
              "title": "E-mail já cadastrado",
              "status": 409,
              "detail": "Já existe uma conta com o e-mail maria@exemplo.com",
              "instance": "/api/v1/auth/registro"
            }""";

    static final String USUARIO_NAO_ENCONTRADO = """
            {
              "title": "Usuário não encontrado",
              "status": 404,
              "detail": "Usuário com id 6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e não encontrado",
              "instance": "/api/v1/usuarios/6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e"
            }""";

    static final String OPERACAO_NAO_PERMITIDA = """
            {
              "title": "Operação não permitida",
              "status": 422,
              "detail": "Você não pode remover o seu próprio acesso de administrador",
              "instance": "/api/v1/usuarios/6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e"
            }""";

    static final String ACESSO_INVALIDO = """
            {
              "title": "Parâmetro inválido",
              "status": 400,
              "detail": "O módulo 'usuarios' é exclusivo de administradores",
              "instance": "/api/v1/usuarios/6f1c2b0e-3d4a-4b5c-8d9e-0f1a2b3c4d5e"
            }""";

    static final String SERVICO_INDISPONIVEL = """
            {
              "title": "Serviço de usuários indisponível",
              "status": 503,
              "detail": "Serviço de usuários indisponível. Tente novamente.",
              "instance": "/api/v1/auth/login"
            }""";

    static final String IMAGEM_INVALIDA = """
            {
              "title": "Parâmetro inválido",
              "status": 400,
              "detail": "Formato não suportado. Envie uma imagem PNG, JPEG ou WEBP",
              "instance": "/api/v1/usuarios/me/avatar"
            }""";

    static final String ARQUIVO_GRANDE = """
            {
              "title": "Arquivo muito grande",
              "status": 413,
              "detail": "A imagem deve ter no máximo 2 MB",
              "instance": "/api/v1/usuarios/me/avatar"
            }""";

    static final String DADOS_INDISPONIVEIS = """
            {
              "title": "Serviço de dados indisponível",
              "status": 503,
              "detail": "Serviço de dados indisponível. Tente novamente.",
              "instance": "/api/v1/demandas"
            }""";

    static final String DEMANDA_NAO_ENCONTRADA = """
            {
              "title": "Demanda não encontrada",
              "status": 404,
              "detail": "Demanda com id 7a2d3c1b-5e4f-4a6b-9c8d-1e2f3a4b5c6d não encontrada",
              "instance": "/api/v1/demandas/7a2d3c1b-5e4f-4a6b-9c8d-1e2f3a4b5c6d"
            }""";

    static final String EXCLUSAO_DE_DEMANDA = """
            {
              "title": "Operação não permitida",
              "status": 422,
              "detail": "Só quem criou a demanda ou um administrador pode excluí-la",
              "instance": "/api/v1/demandas/7a2d3c1b-5e4f-4a6b-9c8d-1e2f3a4b5c6d"
            }""";

    static final String RESPONSAVEL_INVALIDO = """
            {
              "title": "Parâmetro inválido",
              "status": 400,
              "detail": "Responsável não encontrado: 9b8c7d6e-5f4a-4b3c-8d2e-1f0a9b8c7d6e",
              "instance": "/api/v1/demandas"
            }""";

    static final String CONTA_NAO_ENCONTRADA = """
            {
              "title": "Conta não encontrada",
              "status": 404,
              "detail": "Conta com id 8c3e4d2a-6f5b-4c7d-8e9f-2a3b4c5d6e7f não encontrada",
              "instance": "/api/v1/financeiro/contas-pagar/8c3e4d2a-6f5b-4c7d-8e9f-2a3b4c5d6e7f"
            }""";

    static final String CHAVE_PIX_OBRIGATORIA = """
            {
              "title": "Parâmetro inválido",
              "status": 400,
              "detail": "Informe a chave Pix quando a forma de pagamento for PIX",
              "instance": "/api/v1/financeiro/contas-pagar"
            }""";

    static final String CONTA_JA_QUITADA = """
            {
              "title": "Operação não permitida",
              "status": 422,
              "detail": "Só contas pendentes podem receber baixa ou ser canceladas",
              "instance": "/api/v1/financeiro/contas-pagar/8c3e4d2a-6f5b-4c7d-8e9f-2a3b4c5d6e7f/baixa"
            }""";

    private ExemplosDeErro() {
    }
}
