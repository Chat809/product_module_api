# Produtos API

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen)
![Testes](https://img.shields.io/badge/testes-28%20passando-success)
![Cobertura](https://img.shields.io/badge/cobertura%20de%20linhas-100%25-success)

API REST para gerenciamento de produtos (criação, consulta, atualização e remoção), desenvolvida como teste técnico para a vaga de Desenvolvedor Backend Java.

## Requisitos atendidos

| Requisito do teste | Onde está |
|---|---|
| Criar produto com nome, descrição, preço e quantidade em estoque | `POST /api/v1/produtos` |
| Atualizar um produto existente | `PUT /api/v1/produtos/{id}` |
| Remover um produto existente | `DELETE /api/v1/produtos/{id}` |
| Consultar todos os produtos e um produto por id | `GET /api/v1/produtos` e `GET /api/v1/produtos/{id}` |
| Boas práticas RESTful | Recursos no plural, verbos HTTP, status corretos (200, 201 + `Location`, 204, 400, 404) |
| Spring Boot | Spring Boot 4.1.2 |
| Banco de dados em memória | H2 |
| Padrão DAO | `dao/ProdutoDAO` e `dao/JpaProdutoDAO` |
| Padrão Factory para criar os DAOs | `factory/DAOFactory` |
| Padrão DTO nas requisições e respostas | `dto/ProdutoRequestDTO` e `dto/ProdutoResponseDTO` |
| Testes unitários dos serviços | `service/ProdutoServiceTest` (criar, atualizar, remover e consultar) |

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

A aplicação sobe em `http://localhost:8080` com o perfil `dev`.

| Recurso | URL |
|---|---|
| API | http://localhost:8080/api/v1/produtos |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| Console H2 (somente `dev`) | http://localhost:8080/h2-console |

Para o console H2: JDBC URL `jdbc:h2:mem:produtosdb`, usuário `sa`, senha em branco.

Os dados ficam apenas em memória e são perdidos quando a aplicação é reiniciada.

## Como testar

```powershell
.\mvnw.cmd clean verify     # Windows
./mvnw clean verify         # Linux/macOS
```

O comando executa todos os testes e gera o relatório de cobertura em `target/site/jacoco/index.html`.

| Classe de teste | Tipo | O que cobre |
|---|---|---|
| `ProdutoServiceTest` | Unitário (Mockito) | Criação, atualização, remoção e consulta, incluindo produto inexistente |
| `ProdutoControllerTest` | Camada web (`@WebMvcTest`) | Status HTTP, header `Location`, validação e formato dos erros |
| `JpaProdutoDAOTest` | Integração com H2 (`@DataJpaTest`) | Operações reais de persistência do DAO |
| `ProdutosApiApplicationTests` | Contexto | Inicialização da aplicação |

## Endpoints

Base: `/api/v1/produtos`

| Método | Rota | Descrição | Sucesso | Erros |
|---|---|---|---|---|
| `GET` | `/` | Lista todos os produtos | 200 | — |
| `GET` | `/{id}` | Busca um produto pelo id | 200 | 400, 404 |
| `POST` | `/` | Cria um produto | 201 + `Location` | 400 |
| `PUT` | `/{id}` | Atualiza todos os dados de um produto | 200 | 400, 404 |
| `DELETE` | `/{id}` | Remove um produto | 204 | 400, 404 |

Corpo de criação e atualização:

```json
{
  "nome": "Teclado mecânico",
  "descricao": "Teclado mecânico ABNT2 com switches marrons",
  "preco": 349.90,
  "quantidadeEmEstoque": 25
}
```

| Campo | Regras |
|---|---|
| `nome` | Obrigatório, até 120 caracteres |
| `descricao` | Opcional, até 500 caracteres |
| `preco` | Obrigatório, maior que zero, até 2 casas decimais |
| `quantidadeEmEstoque` | Obrigatório, maior ou igual a zero |

### Exemplos com curl

```bash
# Criar
curl -i -X POST http://localhost:8080/api/v1/produtos \
  -H "Content-Type: application/json" \
  -d '{"nome":"Teclado mecânico","descricao":"ABNT2","preco":349.90,"quantidadeEmEstoque":25}'

# Listar
curl http://localhost:8080/api/v1/produtos

# Buscar por id
curl http://localhost:8080/api/v1/produtos/1

# Atualizar
curl -X PUT http://localhost:8080/api/v1/produtos/1 \
  -H "Content-Type: application/json" \
  -d '{"nome":"Teclado mecânico RGB","descricao":"ABNT2","preco":399.90,"quantidadeEmEstoque":20}'

# Remover
curl -i -X DELETE http://localhost:8080/api/v1/produtos/1
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

## Arquitetura

```
br.com.triboraizes.produtosapi
├── config        Configuração do OpenAPI
├── controller    Endpoints REST (somente HTTP, sem regra de negócio)
├── service       Regras de negócio e transações
├── dao           Contrato (ProdutoDAO) e implementação JPA (JpaProdutoDAO)
├── factory       DAOFactory: criação dos DAOs
├── dto           Dados de entrada e saída da API
├── model         Entidade JPA Produto
└── exception     Exceção de domínio e tratamento global de erros
```

Fluxo de uma requisição: `Controller → Service → DAOFactory/ProdutoDAO → JPA (EntityManager) → H2`. Cada camada conhece apenas a camada imediatamente abaixo.

### Padrões de projeto aplicados

- **DAO** (`dao/`): `ProdutoDAO` define as operações de persistência sem expor a tecnologia utilizada. `JpaProdutoDAO` as implementa com o `EntityManager` do JPA, sem depender do Spring.
- **Factory** (`factory/DAOFactory`): é o único ponto que conhece a implementação concreta do DAO. Mesmo com a injeção de dependência do Spring, a factory centraliza a decisão de *qual* DAO criar: o serviço depende apenas da interface `ProdutoDAO`, e trocar a persistência (por exemplo, JDBC puro ou outro banco) exige alterar somente a factory.
- **DTO** (`dto/`): `ProdutoRequestDTO` (entrada, com validações) e `ProdutoResponseDTO` (saída) desacoplam o contrato da API da entidade. A entidade nunca é exposta, e o cliente não consegue definir campos que não deveria, como o `id`.

### Boas práticas adotadas

- Injeção de dependência por construtor e campos `final`.
- Entidade sem setters públicos: alterações passam pelo método de domínio `Produto.atualizar(...)`.
- Transações declaradas no serviço, somente leitura por padrão.
- `open-in-view` desativado: o acesso ao banco fica restrito à camada de serviço.
- Erros padronizados (RFC 9457) e sem exposição de stack trace.
- Perfis `dev` e `prod`: o console H2 fica disponível apenas em desenvolvimento.

### Decisões de design

- `PUT` substitui todos os dados do produto e retorna 404 se o id não existir (não cria o recurso, pois o id é gerado pelo banco).
- `DELETE` de um produto já removido retorna 404.
- A listagem não é paginada, pois o requisito pede a consulta de todos os produtos. A paginação é uma evolução natural caso o volume cresça.
- CORS não foi configurado, pois não há frontend consumindo a API.
- A API é versionada pela URL (`/api/v1`), permitindo evoluir o contrato sem quebrar clientes.

## Deploy no Render

O projeto inclui um `Dockerfile` multi-stage (build com JDK 21 e execução com JRE 21 Alpine, usuário não-root e perfil `prod`).

1. No Render, crie um **Web Service** a partir do repositório.
2. Selecione o runtime **Docker** (o `Dockerfile` da raiz é detectado automaticamente).
3. Em **Health Check Path**, informe `/actuator/health`.
4. Não é necessário definir variáveis de ambiente: a porta é lida da variável `PORT` fornecida pelo Render.

Limitações do plano gratuito:

- O serviço "dorme" após cerca de 15 minutos sem requisições; a primeira chamada seguinte pode levar perto de um minuto.
- Como o banco é em memória, os dados são reiniciados sempre que o serviço reinicia.

Para executar o container localmente:

```bash
docker build -t produtos-api .
docker run -p 8080:8080 produtos-api
```

API publicada: _link a ser adicionado após o deploy._
