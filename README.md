# Produtos API

[![CI](https://github.com/Chat809/product_module_api/actions/workflows/ci.yml/badge.svg)](https://github.com/Chat809/product_module_api/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen)
![Testes](https://img.shields.io/badge/testes-64%20passando-success)

API REST para gerenciamento de produtos e do seu estoque, desenvolvida como teste técnico para a vaga de Desenvolvedor Backend Java. Além do CRUD pedido, oferece paginação com busca e ordenação, movimentações de estoque com histórico, indicadores para dashboard e controle de edições concorrentes.

**Teste online:** [Swagger UI no Render](https://product-module-api.onrender.com/swagger-ui.html) | [Painel administrativo (Angular) na Vercel](https://productmoduleinterface.vercel.app) ([repositório do painel](https://github.com/Chat809/product_module_interface))

## Requisitos atendidos

| Requisito do teste | Onde está |
|---|---|
| Criar produto com nome, descrição, preço e quantidade em estoque | `POST /api/v1/produtos` |
| Atualizar um produto existente | `PUT /api/v1/produtos/{id}` |
| Remover um produto existente | `DELETE /api/v1/produtos/{id}` |
| Consultar todos os produtos e um produto por id | `GET /api/v1/produtos` (paginado) e `GET /api/v1/produtos/{id}` |
| Boas práticas RESTful | Recursos no plural, sub-recurso para movimentações, verbos HTTP e status corretos (200, 201 + `Location`, 204, 400, 404, 409, 422) |
| Spring Boot | Spring Boot 4.1.2 |
| Banco de dados em memória | H2 |
| Padrão DAO | `dao/ProdutoDAO` + `dao/JpaProdutoDAO` e `dao/MovimentacaoDAO` + `dao/JpaMovimentacaoDAO` |
| Padrão Factory para criar os DAOs | `factory/DAOFactory` |
| Padrão DTO nas requisições e respostas | `dto/` (records de entrada com validação e de saída) |
| Testes unitários dos serviços | `service/ProdutoServiceTest` e `service/MovimentacaoServiceTest` |

## Funcionalidades além do pedido

- **Paginação, busca e ordenação** na listagem: `?busca=teclado&page=0&size=20&sort=preco,desc`.
- **Identificadores UUID**, que não expõem a quantidade de registros nem permitem adivinhar ids.
- **Auditoria:** `criadoEm` e `atualizadoEm` preenchidos automaticamente.
- **Controle de concorrência:** cada produto tem uma `versao` (`@Version`). Se o cliente envia uma versão desatualizada no `PUT`, a API responde 409 em vez de sobrescrever a alteração de outra pessoa.
- **Movimentações de estoque:** entradas e saídas com motivo e histórico. Uma saída maior que o estoque é recusada com 422. A criação registra o estoque inicial, e uma edição que muda a quantidade gera um `AJUSTE` automático, mantendo o histórico consistente.
- **Resumo do estoque** para dashboards: total de produtos e de itens, valor em estoque, esgotados e estoque baixo.
- **Dados iniciais:** a aplicação sobe com 15 produtos de exemplo (desligável com `DADOS_INICIAIS=false`).
- **CI** com GitHub Actions executando todos os testes a cada push.

## Stack

- Java 21 (LTS)
- Spring Boot 4.1.2 (Spring Web MVC, Spring Data JPA, Bean Validation, Actuator)
- H2 Database (em memória)
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, AssertJ e JaCoCo
- Maven (via Maven Wrapper)

## Pré-requisitos

- **JDK 21** instalado, com `JAVA_HOME` configurado ou `java` disponível no `PATH`.
- O Maven **não** precisa estar instalado: o projeto usa o Maven Wrapper (`mvnw` / `mvnw.cmd`), que baixa a versão correta automaticamente.

## Como executar

Clone o repositório:

```bash
git clone https://github.com/Chat809/product_module_api.git
cd product_module_api
```

Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080` com o perfil `dev` e 15 produtos de exemplo.

| Recurso | URL |
|---|---|
| API | http://localhost:8080/api/v1/produtos |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| Console H2 (somente `dev`) | http://localhost:8080/h2-console |

Para o console H2: JDBC URL `jdbc:h2:mem:produtosdb`, usuário `sa`, senha em branco.

Os dados ficam apenas em memória e voltam ao estado inicial quando a aplicação é reiniciada.

## Como testar

```powershell
.\mvnw.cmd clean verify     # Windows
./mvnw clean verify         # Linux/macOS
```

O comando executa todos os testes e gera o relatório de cobertura em `target/site/jacoco/index.html`.

| Classe de teste | Tipo | O que cobre |
|---|---|---|
| `ProdutoServiceTest` | Unitário (Mockito) | CRUD, paginação, validação dos parâmetros, conflito de versão, estoque inicial, ajuste automático e resumo |
| `MovimentacaoServiceTest` | Unitário (Mockito) | Entrada, saída, estoque insuficiente, ajuste manual recusado e histórico |
| `ProdutoControllerTest` | Camada web (`@WebMvcTest`) | Status HTTP, `Location`, validação, 400/404/409, paginação, resumo e CORS |
| `MovimentacaoControllerTest` | Camada web (`@WebMvcTest`) | 201, 400, 422 e histórico paginado |
| `JpaProdutoDAOTest` | Integração com H2 (`@DataJpaTest`) | Persistência, auditoria, versão, paginação, busca e consultas agregadas |
| `JpaMovimentacaoDAOTest` | Integração com H2 (`@DataJpaTest`) | Persistência, histórico por produto e remoção |
| `ProdutosApiApplicationTests` | Contexto | Inicialização da aplicação com os dados iniciais |

## Endpoints

### Produtos (`/api/v1/produtos`)

| Método | Rota | Descrição | Sucesso | Erros |
|---|---|---|---|---|
| `GET` | `/` | Lista os produtos com paginação, busca e ordenação | 200 | 400 |
| `GET` | `/resumo` | Indicadores do estoque | 200 | — |
| `GET` | `/{id}` | Busca um produto pelo id | 200 | 400, 404 |
| `POST` | `/` | Cria um produto | 201 + `Location` | 400 |
| `PUT` | `/{id}` | Atualiza todos os dados de um produto | 200 | 400, 404, 409 |
| `DELETE` | `/{id}` | Remove um produto e o seu histórico | 204 | 400, 404 |

Parâmetros da listagem:

| Parâmetro | Padrão | Regras |
|---|---|---|
| `busca` | — | Trecho do nome ou da descrição, sem diferenciar maiúsculas |
| `page` | `0` | A partir de 0 |
| `size` | `20` | De 1 a 500 |
| `sort` | `nome,asc` | `coluna,asc` ou `coluna,desc`. Colunas: `nome`, `preco`, `quantidadeEmEstoque`, `criadoEm`, `atualizadoEm` |

Resposta da listagem:

```json
{
  "conteudo": [
    {
      "id": "3f2b8c1e-6a4d-4e2b-9c1a-7d5e8f9a0b1c",
      "nome": "Teclado mecânico",
      "descricao": "Teclado ABNT2 com switches marrons e iluminação RGB",
      "preco": 349.90,
      "quantidadeEmEstoque": 30,
      "criadoEm": "2026-11-03T12:00:00Z",
      "atualizadoEm": "2026-11-03T12:05:00Z",
      "versao": 2
    }
  ],
  "pagina": 0,
  "tamanho": 20,
  "totalElementos": 1,
  "totalPaginas": 1
}
```

Corpo de criação e atualização:

```json
{
  "nome": "Teclado mecânico",
  "descricao": "Teclado mecânico ABNT2 com switches marrons",
  "preco": 349.90,
  "quantidadeEmEstoque": 25,
  "versao": 2
}
```

| Campo | Regras |
|---|---|
| `nome` | Obrigatório, até 120 caracteres |
| `descricao` | Opcional, até 500 caracteres |
| `preco` | Obrigatório, maior que zero, até 2 casas decimais |
| `quantidadeEmEstoque` | Obrigatório, maior ou igual a zero |
| `versao` | Opcional, usado no `PUT`: a versão que o cliente leu. Se o produto já estiver em outra versão, a resposta é 409 |

Resposta do resumo:

```json
{
  "totalProdutos": 15,
  "totalItens": 216,
  "valorTotal": 118132.30,
  "esgotados": 2,
  "estoqueBaixo": 5,
  "limiteEstoqueBaixo": 5
}
```

O limite de estoque baixo é configurável em `app.estoque.limite-baixo`.

### Movimentações de estoque (`/api/v1/produtos/{id}/movimentacoes`)

| Método | Rota | Descrição | Sucesso | Erros |
|---|---|---|---|---|
| `POST` | `/` | Registra uma entrada ou saída | 201 | 400, 404, 422 |
| `GET` | `/` | Histórico do produto, do mais recente para o mais antigo (`page`, `size` de 1 a 100) | 200 | 400, 404 |

```json
{ "tipo": "SAIDA", "quantidade": 5, "motivo": "Venda para cliente corporativo" }
```

`tipo` aceita `ENTRADA` ou `SAIDA`. Movimentações do tipo `AJUSTE` são criadas pela própria API quando a edição de um produto altera a quantidade.

### Exemplos com curl

```bash
# Listar a segunda página dos mais caros que contenham "mouse"
curl "http://localhost:8080/api/v1/produtos?busca=mouse&page=1&size=10&sort=preco,desc"

# Criar
curl -i -X POST http://localhost:8080/api/v1/produtos \
  -H "Content-Type: application/json" \
  -d '{"nome":"Teclado mecânico","descricao":"ABNT2","preco":349.90,"quantidadeEmEstoque":25}'

# Atualizar (use o id e a versao retornados na criação)
curl -X PUT http://localhost:8080/api/v1/produtos/{id} \
  -H "Content-Type: application/json" \
  -d '{"nome":"Teclado mecânico RGB","descricao":"ABNT2","preco":399.90,"quantidadeEmEstoque":20,"versao":0}'

# Registrar uma saída de estoque
curl -i -X POST http://localhost:8080/api/v1/produtos/{id}/movimentacoes \
  -H "Content-Type: application/json" \
  -d '{"tipo":"SAIDA","quantidade":5,"motivo":"Venda"}'

# Resumo do estoque
curl http://localhost:8080/api/v1/produtos/resumo

# Remover
curl -i -X DELETE http://localhost:8080/api/v1/produtos/{id}
```

### Formato dos erros

Os erros seguem o padrão [RFC 9457 (Problem Details)](https://www.rfc-editor.org/rfc/rfc9457). Exemplo de erro de validação:

```json
{
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos são inválidos",
  "instance": "/api/v1/produtos",
  "erros": [
    { "campo": "nome", "mensagem": "O nome é obrigatório" },
    { "campo": "preco", "mensagem": "O preço deve ser maior que zero" }
  ]
}
```

Outros exemplos:

```json
{ "title": "Conflito de edição", "status": 409,
  "detail": "O produto 3f2b8c1e-... foi alterado por outra pessoa. Recarregue os dados e tente novamente." }

{ "title": "Estoque insuficiente", "status": 422,
  "detail": "Estoque insuficiente: há 3 unidade(s) disponível(is) e a saída pedida é de 5" }
```

## Arquitetura

```
br.com.triboraizes.produtosapi
├── config        OpenAPI, CORS e carga dos dados iniciais
├── controller    Endpoints REST (somente HTTP, sem regra de negócio)
├── service       Regras de negócio e transações
├── dao           Contratos (ProdutoDAO, MovimentacaoDAO) e implementações JPA
├── factory       DAOFactory: criação dos DAOs
├── dto           Dados de entrada e saída da API
├── model         Entidades JPA (Produto, MovimentacaoEstoque) e objetos de valor
└── exception     Exceções de domínio e tratamento global de erros
```

Fluxo de uma requisição: `Controller → Service → DAOFactory/DAO → JPA (EntityManager) → H2`. Cada camada conhece apenas a camada imediatamente abaixo.

### Padrões de projeto aplicados

- **DAO** (`dao/`): as interfaces definem as operações de persistência sem expor a tecnologia utilizada. As implementações `Jpa*DAO` usam o `EntityManager` do JPA com JPQL, sem depender do Spring.
- **Factory** (`factory/DAOFactory`): é o único ponto que conhece as implementações concretas dos DAOs. Os serviços dependem apenas das interfaces, e trocar a persistência (por exemplo, JDBC puro ou outro banco) exige alterar somente a factory.
- **DTO** (`dto/`): records de entrada (com validações) e de saída desacoplam o contrato da API das entidades. As entidades nunca são expostas, e o cliente não consegue definir campos que não deveria, como `id`, datas ou saldo.

### Boas práticas adotadas

- Injeção de dependência por construtor e campos `final`.
- Entidades sem setters públicos: as alterações passam por métodos de domínio (`atualizar`, `registrarEntrada`, `registrarSaida`), e a regra de estoque não negativo fica na própria entidade.
- Transações declaradas nos serviços, somente leitura por padrão.
- `open-in-view` desativado: o acesso ao banco fica restrito à camada de serviço.
- Ordenação dinâmica limitada a uma lista fechada de colunas, sem risco de injeção na consulta.
- Erros padronizados (RFC 9457) e sem exposição de stack trace.
- Perfis `dev` e `prod`: o console H2 fica disponível apenas em desenvolvimento.

### Decisões de design

- `PUT` substitui todos os dados do produto e retorna 404 se o id não existir (não cria o recurso, pois o id é gerado pela API).
- A `versao` é opcional no `PUT` para não quebrar clientes simples. Quem a envia ganha proteção contra sobrescrever edições de outras pessoas. Edições realmente simultâneas também são detectadas pelo `@Version` e retornam 409.
- A listagem tem tamanho máximo de 500 itens por página, o suficiente para o painel carregar o catálogo de uma vez e filtrar no navegador, sem permitir respostas ilimitadas.
- Movimentações são um sub-recurso do produto (`/produtos/{id}/movimentacoes`) e são imutáveis: corrigir um erro é registrar uma nova movimentação.
- `DELETE` remove o produto junto com o seu histórico; um produto já removido retorna 404.
- CORS liberado apenas para `/api/**` e para as origens configuradas em `app.cors.allowed-origins` (`config/CorsConfig`): o frontend local (`http://localhost:4200`) e os deploys do painel na Vercel (`https://productmoduleinterface*.vercel.app` e `https://product-module-interface*.vercel.app`). Outras origens são recusadas.
- A API é versionada pela URL (`/api/v1`), permitindo evoluir o contrato sem quebrar clientes.

## Deploy no Render

O projeto inclui um `Dockerfile` multi-stage (build com JDK 21 e execução com JRE 21 Alpine, usuário não-root e perfil `prod`).

1. No Render, crie um **Web Service** a partir do repositório.
2. Selecione o runtime **Docker** (o `Dockerfile` da raiz é detectado automaticamente).
3. Em **Health Check Path**, informe `/actuator/health`.
4. Não é necessário definir variáveis de ambiente: a porta é lida da variável `PORT` fornecida pelo Render.
5. Opcional: para liberar o CORS para outro domínio de frontend, defina `CORS_ALLOWED_ORIGINS` com as origens separadas por vírgula (por exemplo, `http://localhost:4200,https://meu-painel.vercel.app`). O valor substitui a lista padrão.
6. Opcional: defina `DADOS_INICIAIS=false` para a aplicação subir sem os produtos de exemplo.

Limitações do plano gratuito:

- O serviço "dorme" após cerca de 15 minutos sem requisições; a primeira chamada seguinte pode levar perto de um minuto.
- Como o banco é em memória, os dados voltam ao estado inicial sempre que o serviço reinicia.

Para executar o container localmente:

```bash
docker build -t produtos-api .
docker run -p 8080:8080 produtos-api
```

## API publicada

| Recurso | URL |
|---|---|
| API | https://product-module-api.onrender.com/api/v1/produtos |
| Swagger UI | https://product-module-api.onrender.com/swagger-ui.html |
| Health check | https://product-module-api.onrender.com/actuator/health |
| Painel administrativo (frontend) | https://productmoduleinterface.vercel.app |

No plano gratuito, a primeira requisição após um período sem uso pode levar cerca de um minuto enquanto o serviço inicia.
