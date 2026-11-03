# Produtos API

[![CI](https://github.com/Chat809/product_module_api/actions/workflows/ci.yml/badge.svg)](https://github.com/Chat809/product_module_api/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen)
![Testes](https://img.shields.io/badge/testes-156%20passando-success)
![Cobertura](https://img.shields.io/badge/cobertura%20de%20linhas-99%25-success)

API REST para gerenciamento de produtos e do seu estoque, desenvolvida como teste técnico para a vaga de Desenvolvedor Backend Java. Além do CRUD pedido, oferece paginação com busca e ordenação, movimentações de estoque com histórico, indicadores para dashboard, controle de edições concorrentes com `ETag`, login com e-mail e senha ou com Google, e papéis e acessos por módulo (usuários guardados no Supabase).

## Sumário

- [Links rápidos](#links-rápidos)
- [Requisitos atendidos](#requisitos-atendidos)
- [Funcionalidades](#funcionalidades)
- [Stack](#stack) · [Pré-requisitos](#pré-requisitos) · [Como executar](#como-executar) · [Como testar](#como-testar)
- [Endpoints](#endpoints)
  - [Autenticação](#autenticação) e [Login com Google](#login-com-google)
  - [Usuários](#usuários-apiv1usuarios) e [Acessos por módulo](#acessos-por-módulo)
  - [Produtos](#produtos-apiv1produtos), [ETag](#edição-concorrente-com-etag) e [Movimentações](#movimentações-de-estoque-apiv1produtosidmovimentacoes)
  - [Exemplos com curl](#exemplos-com-curl) e [Formato dos erros](#formato-dos-erros)
- [Arquitetura](#arquitetura) · [Segurança](#segurança) · [Deploy no Render](#deploy-no-render) · [Referências](#referências)

## Links rápidos

| O quê | Link |
|---|---|
| Swagger UI (testar os endpoints no navegador) | https://product-module-api.onrender.com/swagger-ui.html |
| API publicada | https://product-module-api.onrender.com/api/v1/produtos |
| Health check | https://product-module-api.onrender.com/actuator/health |
| Painel administrativo (Angular) que consome esta API | https://productmoduleinterface.vercel.app |
| Repositório do painel | https://github.com/Chat809/product_module_interface |
| Execuções do CI | https://github.com/Chat809/product_module_api/actions |

> No plano gratuito do Render, a primeira requisição depois de um tempo sem uso pode levar até um minuto enquanto o serviço inicia.

## Requisitos atendidos

| Requisito do teste | Onde está |
|---|---|
| Criar produto com nome, descrição, preço e quantidade em estoque | `POST /api/v1/produtos` |
| Atualizar um produto existente | `PUT /api/v1/produtos/{id}` |
| Remover um produto existente | `DELETE /api/v1/produtos/{id}` |
| Consultar todos os produtos e um produto por id | `GET /api/v1/produtos` (paginado) e `GET /api/v1/produtos/{id}` |
| Boas práticas RESTful | Veja [REST e SOLID no projeto](#rest-e-solid-no-projeto) |
| Spring Boot | Spring Boot 4.1.2 |
| Banco de dados em memória | H2 |
| Padrão DAO | `dao/ProdutoDAO` + `dao/JpaProdutoDAO` e `dao/MovimentacaoDAO` + `dao/JpaMovimentacaoDAO` |
| Padrão Factory para criar os DAOs | `factory/DAOFactory` |
| Padrão DTO nas requisições e respostas | `dto/` (records de entrada com validação e de saída) |
| Testes unitários dos serviços | `service/ProdutoServiceTest` e `service/MovimentacaoServiceTest`, além dos testes de domínio, web e persistência |

## Funcionalidades

| Área | O que a API oferece |
|---|---|
| **Produtos** | CRUD completo, paginação, busca por nome ou descrição e ordenação por coluna (`?busca=teclado&page=0&size=20&sort=preco,desc`). Ids UUID e auditoria automática (`criadoEm`, `atualizadoEm`). 15 produtos de exemplo na subida (`DADOS_INICIAIS=false` desliga) |
| **Edição concorrente** | `GET /{id}` devolve o `ETag` com a versão; o `PUT` com `If-Match` recebe 409 se outra pessoa alterou o produto, em vez de sobrescrever. Veja [ETag](#edição-concorrente-com-etag) |
| **Estoque** | Entradas e saídas com motivo e histórico. Saída maior que o estoque responde 422. A criação registra o estoque inicial e a edição da quantidade gera um `AJUSTE` automático |
| **Dashboard** | Resumo com total de produtos e de itens, valor em estoque, esgotados e estoque baixo |
| **Login** | Registro e login com e-mail e senha, ou [login com Google](#login-com-google). Contas que já existem entram com o Google pelo mesmo e-mail, mantendo cargo e acessos. Sessão renovada pelo `refreshToken` |
| **Papéis e acessos** | Papel `ADMIN` ou `USUARIO` e uma lista de módulos liberados (`dashboard`, `produtos` ou `["*"]`). A API recusa com 403 o que não foi liberado. Veja [Acessos por módulo](#acessos-por-módulo) |
| **Gestão de usuários** | Administradores listam usuários e editam nome, e-mail, cargo, acessos e senha de qualquer um. Senhas nunca podem ser lidas, apenas redefinidas |
| **Qualidade** | Swagger com exemplos reais de sucesso e de erro, erros no padrão RFC 9457, 156 testes e CI com cobertura mínima de 80% a cada push |

## Stack

- Java 21 (LTS)
- Spring Boot 4.1.2 (Spring Web MVC, Spring Data JPA, Bean Validation, Actuator)
- Spring Security como OAuth2 Resource Server (validação de JWT)
- H2 Database (em memória) para produtos e movimentações
- Supabase (Auth e Postgres) para usuários, papéis e acessos
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, AssertJ e JaCoCo
- Maven (via Maven Wrapper)

## Pré-requisitos

- **JDK 21** instalado, com `JAVA_HOME` configurado ou `java` disponível no `PATH`.
- O Maven **não** precisa estar instalado: o projeto usa o Maven Wrapper (`mvnw` / `mvnw.cmd`), que baixa a versão correta automaticamente.

## Como executar

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

A aplicação sobe em `http://localhost:8080` (ou na porta da variável `SERVER_PORT`, se definida) com o perfil `dev` e 15 produtos de exemplo.

Os endpoints de produtos exigem login. Para entrar localmente, defina antes a chave secreta do Supabase (em *Project Settings > API Keys*), que nunca deve ir para o repositório:

```powershell
$env:SUPABASE_SECRET_KEY = "sb_secret_..."   # Windows
export SUPABASE_SECRET_KEY="sb_secret_..."   # Linux/macOS
```

Sem a chave, a aplicação sobe normalmente, mas login e registro respondem 503 explicando o que falta.

| Recurso | URL |
|---|---|
| API | http://localhost:8080/api/v1/produtos |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| Console H2 (somente `dev`) | http://localhost:8080/h2-console |

Para o console H2: JDBC URL `jdbc:h2:mem:produtosdb`, usuário `sa`, senha em branco. Os dados ficam apenas em memória e voltam ao estado inicial quando a aplicação é reiniciada.

Para rodar o painel junto, siga o README do [repositório do painel](https://github.com/Chat809/product_module_interface).

## Como testar

```powershell
.\mvnw.cmd clean verify     # Windows
./mvnw clean verify         # Linux/macOS
```

O comando executa os 156 testes, gera o relatório de cobertura em `target/site/jacoco/index.html` e **falha se a cobertura de linhas ficar abaixo de 80%** (hoje está em 99%). Nenhum teste acessa o Supabase de verdade: as chamadas HTTP são simuladas com `MockRestServiceServer`.

| Classe de teste | Tipo | O que cobre |
|---|---|---|
| `ProdutoTest` | Unitário puro (sem Spring) | Regras da entidade: normalização dos dados, entrada, saída, estoque insuficiente e quantidade inválida |
| `ProdutoServiceTest` | Unitário (Mockito) | CRUD, paginação, validação dos parâmetros, conflito de versão, estoque inicial, ajuste automático e resumo |
| `MovimentacaoServiceTest` | Unitário (Mockito) | Entrada, saída, estoque insuficiente, ajuste manual recusado e histórico |
| `GlobalExceptionHandlerTest` | Unitário da camada web (MockMvc standalone) | Formato RFC 9457 de cada erro: 400, 404, 409, 422 e 500 sem vazar detalhes internos |
| `ProdutoControllerTest` | Camada web (`@WebMvcTest`) | Status HTTP, `Location`, `ETag` e `If-Match`, validação, paginação, resumo, CORS, 401 sem token e 403 por módulo (dashboard só lê, sem módulo nada passa) |
| `MovimentacaoControllerTest` | Camada web (`@WebMvcTest`) | 201, 400, 422 e histórico paginado |
| `AuthControllerTest` | Camada web (`@WebMvcTest`) | Registro, login, login com Google e refresh sem token; 400, 401, 409 e 503 |
| `UsuarioControllerTest` | Camada web (`@WebMvcTest`) com JWT simulado | `/me` para qualquer papel, 403 para não-admin, listagem, edição (400, 409, 422) e nova senha (204) |
| `AuthServiceTest` e `UsuarioServiceTest` | Unitário (Mockito) | Normalização do e-mail, sessão com papel e acessos, nome vindo do Google sem sobrepor o do cadastro, edição que só chama o Supabase Auth quando nome ou e-mail mudam, e bloqueio do admin rebaixar a si mesmo |
| `SupabaseAuthClientTest` | Unitário com `MockRestServiceServer` | Corpo e cabeçalhos enviados ao Supabase Auth (criação, edição, nova senha, troca do código do Google) e tradução de cada erro (e-mail repetido, senha fraca, id inexistente, credenciais, código do Google expirado, indisponibilidade) |
| `SupabasePerfilDAOTest` | Unitário com `MockRestServiceServer` | Consultas ao PostgREST, contagem pelo `Content-Range` e gravação de papel e acessos como array JSON |
| `PermissoesDoTokenConverterTest` | Unitário puro | Conversão de papel e acessos do `app_metadata` em `ROLE_*` e `MODULO_*`, inclusive tokens antigos sem acessos |
| `AcessosTest` | Unitário puro | Regras da coluna `acessos`: `*`, ordem canônica, módulo desconhecido e `usuarios` exclusivo de admin |
| `JpaProdutoDAOTest` | Integração com H2 (`@DataJpaTest`) | Persistência, auditoria, versão, paginação, busca e consultas agregadas |
| `JpaMovimentacaoDAOTest` | Integração com H2 (`@DataJpaTest`) | Persistência, histórico por produto e remoção |
| `ProdutosApiApplicationTests` | Contexto | Inicialização da aplicação com os dados iniciais |

Os testes unitários isolam uma peça por vez: a entidade é testada com `new Produto(...)`, os serviços recebem DAOs falsos do Mockito e os controllers sobem apenas a camada web. Os testes de DAO usam o H2 de verdade, porque o que importa ali é a consulta JPQL.

## Endpoints

A documentação completa, com exemplos de requisição e de cada erro, está no [Swagger UI](https://product-module-api.onrender.com/swagger-ui.html).

Com exceção de `/api/v1/auth/**`, do Swagger e do health check, todas as rotas exigem o cabeçalho `Authorization: Bearer <accessToken>`. Sem ele, a resposta é 401. Com ele, mas sem o módulo ou o papel exigido pela rota, a resposta é 403 (veja [Acessos por módulo](#acessos-por-módulo)).

### Autenticação

Os usuários e as senhas ficam no **Supabase Auth**. O painel conversa só com esta API, e a API conversa com o Supabase usando a chave secreta, que nunca sai do servidor. A única exceção é o login com Google, em que o navegador passa pelo Supabase e pelo Google antes de voltar ao painel:

```
Painel (Angular) → API (Spring) → Supabase Auth / Postgres
```

| Método | Rota | Descrição | Sucesso | Erros |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/registro` | Cria a conta (`nome`, `email`, `senha`) com papel `USUARIO` e já abre a sessão | 201 | 400, 409, 503 |
| `POST` | `/api/v1/auth/login` | Abre a sessão com e-mail e senha | 200 | 400, 401, 503 |
| `POST` | `/api/v1/auth/google` | Conclui o [login com Google](#login-com-google): troca `codigo` e `verificador` pela sessão | 200 | 400, 401, 503 |
| `POST` | `/api/v1/auth/refresh` | Troca o `refreshToken` por uma sessão nova | 200 | 400, 401 |

Resposta do login, do registro e do login com Google:

```json
{
  "accessToken": "eyJhbGciOiJFUzI1NiIs...",
  "refreshToken": "v1.MjQ0...",
  "expiraEm": "2026-11-03T13:00:00Z",
  "usuario": {
    "id": "6f1c2b0e-...", "nome": "Maria Souza", "email": "maria@exemplo.com",
    "papel": "USUARIO", "acessos": ["dashboard", "produtos"]
  }
}
```

Como funciona por dentro:

- O `accessToken` é um JWT assinado pelo Supabase com **ES256**. A API valida assinatura (chaves públicas do JWKS do projeto), emissor, audiência `authenticated` e expiração, sem consultar o Supabase a cada requisição.
- O papel e os acessos ficam na tabela `public.perfis` do Supabase. Um trigger copia os dois para o `app_metadata` do usuário, que vai dentro do JWT e que o próprio usuário não consegue alterar. A API converte esses valores em `ROLE_ADMIN` ou `ROLE_USUARIO` e em um `MODULO_*` para cada módulo liberado.
- Novas contas são criadas pela API de administração do Supabase já com o e-mail confirmado, para que a pessoa entre logo depois do registro.

### Login com Google

O login com Google usa o provedor Google do Supabase Auth no fluxo **PKCE** ([RFC 7636](https://www.rfc-editor.org/rfc/rfc7636)). O token nunca aparece na URL: o navegador recebe só um código de uso único, que de nada serve sem o verificador guardado na aba que iniciou o login.

```
1. Painel   gera o verificador (aleatório) e guarda na aba; calcula o desafio = SHA-256(verificador)
2. Painel → Supabase /auth/v1/authorize?provider=google&code_challenge=<desafio>&redirect_to=<painel>/auth/callback
3. Supabase → Google (a pessoa escolhe a conta) → Supabase → <painel>/auth/callback?code=<código>
4. Painel → API   POST /api/v1/auth/google  { "codigo": "<código>", "verificador": "<verificador>" }
5. API → Supabase POST /auth/v1/token?grant_type=pkce  { auth_code, code_verifier }
6. API → Painel   mesma sessão do login com senha (accessToken, refreshToken, usuario com papel e acessos)
```

| Campo | Regras |
|---|---|
| `codigo` | Obrigatório, até 512 caracteres. É o parâmetro `code` do retorno |
| `verificador` | Obrigatório, de 43 a 128 caracteres. O Supabase confere se o hash dele bate com o desafio do passo 2 |

**Quem pode entrar**

- **E-mail que já tem conta** (criada pelo registro ou por um admin, seja `USUARIO` ou `ADMIN`): o Supabase vincula o Google à conta existente, e a pessoa entra nela com o mesmo papel, os mesmos acessos e o mesmo nome. Depois disso, ela pode entrar tanto com a senha quanto com o Google.
- **E-mail novo:** a conta é criada como `USUARIO` com os acessos padrão (`dashboard` e `produtos`), igual ao registro. O nome vem do perfil do Google.
- Um código expirado, já usado ou com verificador errado responde **401** com `"Login com Google expirado ou inválido. Tente novamente."`.

**Configuração (feita uma vez)**

| Onde | O que configurar |
|---|---|
| [Google Cloud: tela de consentimento](https://console.cloud.google.com/auth/branding) | App externo e publicado, com os escopos `openid`, `email` e `profile` |
| [Google Cloud: clientes OAuth](https://console.cloud.google.com/auth/clients) | Cliente do tipo **Aplicativo da Web**. Origens: o domínio do painel e `http://localhost:4200`. URI de redirecionamento: `https://<projeto>.supabase.co/auth/v1/callback` |
| Supabase: *Authentication > Sign In / Providers > Google* | Ativar e colar o Client ID e o Client Secret do Google |
| Supabase: *Authentication > URL Configuration* | Site URL do painel e, em Redirect URLs, `<painel>/auth/callback` e `http://localhost:4200/auth/callback` |

O Client ID e o Client Secret do Google ficam **só no Supabase**. A API não precisa de nenhuma variável nova: ela troca o código usando a mesma `SUPABASE_SECRET_KEY` do login com senha.

### Usuários (`/api/v1/usuarios`)

| Método | Rota | Quem pode | Descrição | Sucesso | Erros |
|---|---|---|---|---|---|
| `GET` | `/me` | Qualquer usuário logado | Dados do usuário logado, com papel e acessos atuais | 200 | 401 |
| `GET` | `/` | `ADMIN` | Lista os usuários em ordem alfabética, com papel e acessos (`page`, `size` de 1 a 100) | 200 | 400, 401, 403 |
| `PUT` | `/{id}` | `ADMIN` | Edita nome, e-mail, papel (cargo) e acessos | 200 | 400, 401, 403, 404, 409, 422 |
| `PUT` | `/{id}/senha` | `ADMIN` | Define uma nova senha (`{"senha": "..."}`, de 6 a 72 caracteres) | 204 | 400, 401, 403, 404 |

Corpo do `PUT /{id}`:

```json
{ "nome": "Maria Souza", "email": "maria@exemplo.com", "papel": "USUARIO", "acessos": ["dashboard"] }
```

- Um administrador não pode remover o próprio acesso de administrador (422), o que garante que sempre reste ao menos um.
- Nome e e-mail ficam no Supabase Auth (é com o e-mail que se entra) e só são enviados a ele quando mudam. Um e-mail que já pertence a outra conta responde 409. O novo e-mail já fica confirmado.
- **Senhas não podem ser consultadas**, nem por administradores: o Supabase guarda apenas o hash (bcrypt). Por isso existe "definir nova senha", e não "ver senha".
- Papel e acessos novos valem para o usuário afetado no próximo login ou renovação de sessão (em até 1 hora).

### Acessos por módulo

Além do papel, cada usuário tem uma lista de **acessos** que define quais módulos do painel ele pode usar. Ela fica na coluna `acessos` (jsonb) da tabela `public.perfis` do Supabase.

**Valores aceitos**

| Valor | Significado |
|---|---|
| `["*"]` | Acesso total: todos os módulos permitidos ao papel |
| `["dashboard", "produtos"]` | Apenas os módulos listados (padrão de toda conta nova) |
| `["dashboard"]` | Só o dashboard |
| `[]` | Nenhum módulo: a pessoa entra, mas o painel mostra "Nenhum módulo liberado" |

Módulos existentes: `dashboard`, `produtos` e `usuarios`.

**Regras**

- `ADMIN` tem sempre `["*"]`. Isso é garantido em três lugares: um trigger no banco, a normalização da API e o converter do token, que ignora a lista para administradores.
- O módulo `usuarios` é **exclusivo de administradores**. Para um `USUARIO`, `*` libera todos os módulos exceto esse, e enviar `"usuarios"` na lista responde 400.
- A API grava a forma canônica: `*` prevalece sobre o resto, repetições são removidas e os módulos ficam na ordem do menu (`["produtos", "dashboard", "produtos"]` vira `["dashboard", "produtos"]`).
- Valores desconhecidos são recusados com 400 pela API e por uma `check constraint` no banco.

**O que cada módulo libera na API**

| Rota | Exige |
|---|---|
| `GET /api/v1/produtos/resumo` | `dashboard` |
| `GET /api/v1/produtos/**` (catálogo, produto, histórico) | `dashboard` ou `produtos` (o dashboard mostra a lista do que repor) |
| `POST`, `PUT`, `DELETE` em `/api/v1/produtos/**` (inclusive movimentações) | `produtos` |
| `/api/v1/usuarios/**` (exceto `/me`) | papel `ADMIN` |

**Como funciona por dentro**

```
perfis.acessos ──trigger──▶ auth.users.app_metadata.acessos ──▶ JWT ──▶ PermissoesDoTokenConverter ──▶ MODULO_DASHBOARD, MODULO_PRODUTOS...
```

1. O admin altera os acessos pelo painel (`PUT /api/v1/usuarios/{id}`) ou direto no banco.
2. O trigger `ao_definir_papel` copia papel e acessos para o `app_metadata` do usuário no Supabase Auth.
3. No próximo login ou renovação, o JWT sai com os acessos novos. O `PermissoesDoTokenConverter` transforma cada módulo liberado em uma autoridade `MODULO_*`, que o `SecurityConfig` exige rota a rota.
4. O painel usa a mesma regra para esconder menus e bloquear rotas, mas quem decide de fato é a API.

Tokens emitidos antes da coluna existir não trazem `acessos` e recebem o padrão (`dashboard` e `produtos`), então ninguém perde acesso na migração.

**Alterar direto no banco (SQL Editor do Supabase)**

```sql
-- Limitar uma pessoa ao dashboard
update public.perfis set acessos = '["dashboard"]' where email = 'maria@exemplo.com';

-- Liberar tudo
update public.perfis set acessos = '["*"]' where email = 'maria@exemplo.com';
```

A estrutura da coluna:

```sql
acessos jsonb not null default '["dashboard", "produtos"]'
  check (jsonb_typeof(acessos) = 'array'
         and acessos <@ '["*", "dashboard", "produtos", "usuarios"]'
         and (papel = 'ADMIN' or not acessos ? 'usuarios'))
```

**Adicionar um módulo novo** exige quatro passos: incluir a chave no enum `Modulo` da API, ampliar a `check constraint` da coluna, proteger as rotas dele no `SecurityConfig` com `Modulo.X.autoridade()` e cadastrá-lo na lista `MODULOS` do painel.

### Produtos (`/api/v1/produtos`)

| Método | Rota | Descrição | Sucesso | Erros |
|---|---|---|---|---|
| `GET` | `/` | Lista os produtos com paginação, busca e ordenação | 200 | 400 |
| `GET` | `/resumo` | Indicadores do estoque | 200 | — |
| `GET` | `/{id}` | Busca um produto pelo id | 200 + `ETag` | 400, 404 |
| `POST` | `/` | Cria um produto | 201 + `Location` + `ETag` | 400 |
| `PUT` | `/{id}` | Atualiza todos os dados de um produto (aceita `If-Match`) | 200 + `ETag` | 400, 404, 409 |
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
  "quantidadeEmEstoque": 25
}
```

| Campo | Regras |
|---|---|
| `nome` | Obrigatório, até 120 caracteres |
| `descricao` | Opcional, até 500 caracteres |
| `preco` | Obrigatório, maior que zero, até 2 casas decimais |
| `quantidadeEmEstoque` | Obrigatório, maior ou igual a zero |
| `versao` | Opcional, usado no `PUT` como alternativa ao `If-Match`. Se os dois vierem, vale o cabeçalho |

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

### Edição concorrente com ETag

1. `GET /api/v1/produtos/{id}` responde com `ETag: "3"`.
2. O cliente envia `PUT /api/v1/produtos/{id}` com `If-Match: "3"`.
3. Se ninguém alterou o produto, a resposta é 200 com `ETag: "4"`. Se alguém alterou, a resposta é 409 e nada é sobrescrito.

`If-Match: *` atualiza sem checagem, e um `If-Match` malformado retorna 400. Sem cabeçalho e sem `versao` no corpo, a atualização acontece normalmente, para não quebrar clientes simples.

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
# Criar uma conta (ou use /login com uma conta existente) e guardar o token
curl -X POST http://localhost:8080/api/v1/auth/registro \
  -H "Content-Type: application/json" \
  -d '{"nome":"Maria Souza","email":"maria@exemplo.com","senha":"minhaSenha123"}'
TOKEN="cole aqui o accessToken da resposta"

# Listar a segunda página dos mais caros que contenham "mouse"
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/api/v1/produtos?busca=mouse&page=1&size=10&sort=preco,desc"

# Criar
curl -i -X POST http://localhost:8080/api/v1/produtos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nome":"Teclado mecânico","descricao":"ABNT2","preco":349.90,"quantidadeEmEstoque":25}'

# Ler o produto e a versão atual (veja o cabeçalho ETag)
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/produtos/{id}

# Atualizar somente se ninguém tiver alterado desde a leitura
curl -i -X PUT http://localhost:8080/api/v1/produtos/{id} \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H 'If-Match: "0"' \
  -d '{"nome":"Teclado mecânico RGB","descricao":"ABNT2","preco":399.90,"quantidadeEmEstoque":20}'

# Registrar uma saída de estoque
curl -i -X POST http://localhost:8080/api/v1/produtos/{id}/movimentacoes \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"tipo":"SAIDA","quantidade":5,"motivo":"Venda"}'

# Resumo do estoque
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/produtos/resumo

# Remover
curl -i -X DELETE -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/produtos/{id}

# Dados do usuário logado
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/usuarios/me

# (ADMIN) Limitar um usuário ao dashboard
curl -X PUT http://localhost:8080/api/v1/usuarios/{id} \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"nome": "Maria Souza", "email": "maria@exemplo.com", "papel": "USUARIO", "acessos": ["dashboard"]}'

# (ADMIN) Definir uma nova senha
curl -i -X PUT http://localhost:8080/api/v1/usuarios/{id}/senha \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"senha": "novaSenha123"}'
```

### Formato dos erros

Os erros seguem o padrão [RFC 9457 (Problem Details)](https://www.rfc-editor.org/rfc/rfc9457), com `Content-Type: application/problem+json`. Exemplo de erro de validação:

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

{ "title": "Não autenticado", "status": 401,
  "detail": "Faça login e envie o token no cabeçalho Authorization: Bearer <token>" }

{ "title": "Acesso negado", "status": 403,
  "detail": "Seu usuário não tem permissão para esta operação. Peça a um administrador para liberar o acesso." }
```

## Arquitetura

```
br.com.triboraizes.produtosapi
├── config        OpenAPI, CORS, segurança, Supabase e carga dos dados iniciais
├── controller    Endpoints REST (somente HTTP, sem regra de negócio)
├── service       Regras de negócio e transações
├── dao           Contratos (ProdutoDAO, MovimentacaoDAO, PerfilDAO) e implementações JPA e Supabase
├── factory       DAOFactory: criação dos DAOs JPA
├── client        SupabaseAuthClient: chamadas ao Supabase Auth
├── security      Conversão do papel e dos acessos do JWT em permissões do Spring Security
├── dto           Dados de entrada e saída da API
├── model         Entidades JPA (Produto, MovimentacaoEstoque), Perfil, Papel, Modulo, Acessos e objetos de valor
└── exception     Exceções de domínio e tratamento global de erros
```

Fluxo de uma requisição de produto: `filtro JWT → Controller → Service → DAOFactory/DAO → JPA (EntityManager) → H2`. Fluxo de usuários: `Controller → Service → PerfilDAO / SupabaseAuthClient → API REST do Supabase`. Cada camada conhece apenas a camada imediatamente abaixo.

### Padrões de projeto aplicados

- **DAO** (`dao/`): as interfaces definem as operações de persistência sem expor a tecnologia utilizada. As implementações `Jpa*DAO` usam o `EntityManager` do JPA com JPQL, sem depender do Spring. O `SupabasePerfilDAO` implementa `PerfilDAO` sobre a API REST do Supabase, e o `UsuarioService` não sabe disso.
- **Factory** (`factory/DAOFactory`): é o único ponto que conhece as implementações concretas dos DAOs. Os serviços dependem apenas das interfaces, e trocar a persistência (por exemplo, JDBC puro ou outro banco) exige alterar somente a factory.
- **DTO** (`dto/`): records de entrada (com validações) e de saída desacoplam o contrato da API das entidades. As entidades nunca são expostas, e o cliente não consegue definir campos que não deveria, como `id`, datas ou saldo.

### REST e SOLID no projeto

**REST**

| Princípio | Como foi aplicado |
|---|---|
| Recursos identificados por URL | `/produtos`, `/produtos/{id}` e o sub-recurso `/produtos/{id}/movimentacoes` |
| Verbos com a semântica do HTTP | `GET` sem efeito colateral; `POST` cria; `PUT` substitui o recurso inteiro; `DELETE` remove |
| Status corretos | 200, 201 com `Location`, 204, 400, 404, 409 e 422 |
| Controle de concorrência do HTTP | `ETag` nas respostas e `If-Match` no `PUT` ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110#name-if-match)) |
| Erros padronizados | `application/problem+json` (RFC 9457) |
| Versionamento | Pela URL (`/api/v1`) |
| Sem estado no servidor | Nenhuma sessão: cada requisição carrega tudo o que precisa, inclusive o JWT |

**SOLID**

| Princípio | Onde aparece |
|---|---|
| **S**: responsabilidade única | `ProdutoController` só traduz HTTP, `ProdutoService` só aplica regras e transações, `JpaProdutoDAO` só persiste, `GlobalExceptionHandler` só formata erros |
| **O**: aberto para extensão, fechado para modificação | Uma nova persistência é uma nova implementação de `ProdutoDAO`, criada pela `DAOFactory`, sem mexer nos serviços. Um novo erro é uma nova exceção com seu handler |
| **L**: substituição de Liskov | Qualquer implementação de `ProdutoDAO` ou `MovimentacaoDAO` pode substituir a JPA: os testes de serviço provam isso usando DAOs falsos |
| **I**: segregação de interfaces | Um DAO por agregado (`ProdutoDAO` e `MovimentacaoDAO`), cada um só com as operações que os serviços usam |
| **D**: inversão de dependência | Os serviços dependem das interfaces dos DAOs e recebem a `DAOFactory` por injeção no construtor; não conhecem `EntityManager` nem JPQL |

### Boas práticas adotadas

- Injeção de dependência por construtor e campos `final`.
- Entidades sem setters públicos: as alterações passam por métodos de domínio (`atualizar`, `registrarEntrada`, `registrarSaida`), e a regra de estoque não negativo fica na própria entidade.
- Transações declaradas nos serviços, somente leitura por padrão.
- `open-in-view` desativado: o acesso ao banco fica restrito à camada de serviço.
- Ordenação dinâmica limitada a uma lista fechada de colunas, sem risco de injeção na consulta.
- Erros padronizados (RFC 9457) e sem exposição de stack trace.
- Perfis `dev` e `prod`: o console H2 fica disponível apenas em desenvolvimento.
- Cobertura mínima verificada no build, para que a qualidade dos testes não regrida.

### Decisões de design

- `PUT` substitui todos os dados do produto e retorna 404 se o id não existir (não cria o recurso, pois o id é gerado pela API).
- A checagem de versão é opcional no `PUT` para não quebrar clientes simples. Quem envia `If-Match` (ou `versao`) ganha proteção contra sobrescrever edições de outras pessoas. Edições realmente simultâneas também são detectadas pelo `@Version` e retornam 409.
- A listagem tem tamanho máximo de 500 itens por página, o suficiente para o painel carregar o catálogo de uma vez e filtrar no navegador, sem permitir respostas ilimitadas.
- Movimentações são um sub-recurso do produto (`/produtos/{id}/movimentacoes`) e são imutáveis: corrigir um erro é registrar uma nova movimentação.
- `DELETE` remove o produto junto com o seu histórico; um produto já removido retorna 404.
- A API é versionada pela URL (`/api/v1`), permitindo evoluir o contrato sem quebrar clientes.

## Segurança

**O que já existe**

- **Autenticação com JWT** emitido pelo Supabase Auth (ES256). A API é um resource server sem estado: valida assinatura, emissor, audiência e expiração de cada token. Senhas nunca passam pelo banco desta API.
- **Login com Google no fluxo PKCE:** o token não trafega na URL, o código de retorno é de uso único e só funciona junto com o verificador que ficou na aba de origem. O Supabase só redireciona para endereços da lista permitida, e o Client Secret do Google fica no Supabase, fora da API e do painel.
- **Autorização por papel e por módulo:** papel (`ADMIN`, `USUARIO`) e acessos são lidos do `app_metadata` do token, que só a chave secreta consegue alterar. Rotas de produtos exigem o módulo correspondente, rotas de gestão de usuários exigem `ADMIN`, e um admin não pode remover o próprio acesso de administrador.
- **Senhas inacessíveis:** ficam só como hash no Supabase Auth. Nem a API nem os administradores conseguem lê-las, apenas definir uma nova.
- **Chave secreta só no servidor:** a `SUPABASE_SECRET_KEY` vem de variável de ambiente, nunca do repositório. O painel não chama o Supabase; no login com Google ele apenas redireciona o navegador.
- **Tabela de perfis protegida por RLS** sem políticas públicas: só o backend, com a chave secreta, lê e escreve.
- **Validação de entrada** com Bean Validation em todos os campos, com limites de tamanho e de valor (inclusive na quantidade de uma movimentação e no tamanho da página).
- **Consultas parametrizadas** em JPQL. A ordenação dinâmica só aceita colunas de uma lista fechada, então não há como injetar SQL pela URL.
- **CORS restrito** a `/api/**` e às origens configuradas em `app.cors.allowed-origins`: o frontend local e os deploys do painel na Vercel. Outras origens recebem 403.
- **Nenhum detalhe interno nas respostas:** erros inesperados retornam uma mensagem genérica, e o stack trace fica só no log do servidor.
- **Superfície mínima em produção:** o console H2 é desligado no perfil `prod`, e o Actuator expõe apenas `/health`, sem detalhes.
- **Container sem privilégios:** a imagem Docker roda com usuário não-root.
- **Ids não sequenciais (UUID)**, que não revelam quantos registros existem nem permitem percorrer a base adivinhando ids.
- **Proteção contra sobrescrita acidental** com `ETag` e `If-Match`.

**O que não existe, e por quê**

| Ausente | Motivo neste projeto | O que seria feito em produção |
|---|---|---|
| Limite de requisições próprio | O Supabase Auth já limita tentativas de login; o restante é um banco em memória sem dado real | Rate limit por IP ou por usuário (Bucket4j ou no gateway), respondendo 429 |
| Revogação imediata de token | O `accessToken` expira em 1 hora e a troca de papel ou de acessos vale na próxima renovação | Tokens mais curtos ou checagem do papel no banco a cada requisição sensível |
| Cofre de segredos | Uma única chave, guardada nas variáveis de ambiente do Render | Vault ou o gerenciador de segredos do provedor de nuvem, com rotação |
| Auditoria de quem alterou | Os produtos ficam no H2 em memória e reiniciam com o serviço | Gravar o `sub` do JWT junto com `criadoEm` e `atualizadoEm` |

## Deploy no Render

O projeto inclui um `Dockerfile` multi-stage (build com JDK 21 e execução com JRE 21 Alpine, usuário não-root e perfil `prod`).

1. No Render, crie um **Web Service** a partir do repositório.
2. Selecione o runtime **Docker** (o `Dockerfile` da raiz é detectado automaticamente).
3. Em **Health Check Path**, informe `/actuator/health`.
4. Em **Environment**, defina `SUPABASE_SECRET_KEY` com a chave secreta do projeto Supabase. A porta é lida da variável `PORT` fornecida pelo Render, e `SUPABASE_URL` já tem como padrão o projeto usado aqui.
5. Opcional: para liberar o CORS para outro domínio de frontend, defina `CORS_ALLOWED_ORIGINS` com as origens separadas por vírgula (por exemplo, `http://localhost:4200,https://meu-painel.vercel.app`). O valor substitui a lista padrão.
6. Opcional: defina `DADOS_INICIAIS=false` para a aplicação subir sem os produtos de exemplo.

Limitações do plano gratuito:

- O serviço "dorme" após cerca de 15 minutos sem requisições; a primeira chamada seguinte pode levar perto de um minuto.
- Como o banco é em memória, os dados voltam ao estado inicial sempre que o serviço reinicia.

Para executar o container localmente:

```bash
docker build -t produtos-api .
docker run -p 8080:8080 -e SUPABASE_SECRET_KEY="sb_secret_..." produtos-api
```

## Referências

- [Spring Boot](https://docs.spring.io/spring-boot/index.html)
- [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html) e [tratamento de erros com ProblemDetail](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Jakarta Persistence (JPA)](https://jakarta.ee/specifications/persistence/) e [Hibernate ORM](https://hibernate.org/orm/documentation/)
- [Jakarta Bean Validation](https://beanvalidation.org/)
- [H2 Database](https://www.h2database.com/)
- [springdoc-openapi](https://springdoc.org/) e [OpenAPI Specification](https://spec.openapis.org/oas/latest.html)
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/), [Mockito](https://site.mockito.org/), [AssertJ](https://assertj.github.io/doc/) e [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/)
- [Spring Security: OAuth2 Resource Server com JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Supabase Auth](https://supabase.com/docs/guides/auth), [chaves de assinatura do JWT](https://supabase.com/docs/guides/auth/signing-keys) e [Row Level Security](https://supabase.com/docs/guides/database/postgres/row-level-security)
- [Supabase: login com Google](https://supabase.com/docs/guides/auth/social-login/auth-google), [fluxo PKCE](https://supabase.com/docs/guides/auth/sessions/pkce-flow) e [vínculo de identidades](https://supabase.com/docs/guides/auth/auth-identity-linking)
- [RFC 7636: PKCE](https://www.rfc-editor.org/rfc/rfc7636)
- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
- [RFC 9110: HTTP Semantics (ETag e If-Match)](https://www.rfc-editor.org/rfc/rfc9110)
- [Render: deploy com Docker](https://render.com/docs/docker)
- [GitHub Actions](https://docs.github.com/actions)
