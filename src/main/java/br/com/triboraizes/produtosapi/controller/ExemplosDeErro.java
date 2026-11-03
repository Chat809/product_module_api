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

    private ExemplosDeErro() {
    }
}
