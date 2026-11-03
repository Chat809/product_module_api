# Produtos API

[![CI](https://github.com/Chat809/product_module_api/actions/workflows/ci.yml/badge.svg)](https://github.com/Chat809/product_module_api/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen)
![Testes](https://img.shields.io/badge/testes-239%20passando-success)
![Cobertura](https://img.shields.io/badge/cobertura%20de%20linhas-98%25-success)

API REST de produtos e estoque feita para o teste técnico de Desenvolvedor Backend Java. Cumpre o que o PDF pede (CRUD de produtos com Spring Boot, H2, DAO, Factory, DTO e testes dos serviços) e, a partir de sugestões feitas durante o desenvolvimento, ganhou autenticação, acessos por módulo, gestão de usuários, perfil com foto, demandas em Kanban e um módulo financeiro.

## Sumário

- [Links rápidos](#links-rápidos)
- [Requisitos do teste técnico (PDF original)](#requisitos-do-teste-técnico-pdf-original)
- [Funcionalidades extras](#funcionalidades-extras): [paginação](#1-paginação-busca-e-ordenação) · [ETag](#2-edição-concorrente-com-etag) · [movimentações](#3-movimentações-de-estoque) · [resumo](#4-resumo-para-dashboard) · [autenticação](#5-autenticação-e-mail-e-senha-e-login-com-google) · [acessos](#6-papéis-e-acessos-por-módulo) · [usuários](#7-gestão-de-usuários-pelo-admin) · [perfil](#8-meu-perfil-e-avatar) · [demandas](#9-demandas-e-tarefas) · [financeiro](#10-financeiro-contas-a-pagar-e-a-receber)
- [Referência técnica](#referência-técnica): [stack](#stack) · [como executar](#como-executar) · [testes](#testes) · [erros](#formato-dos-erros) · [arquitetura](#arquitetura) · [REST e SOLID](#rest-e-solid) · [segurança](#segurança) · [deploy](#deploy-no-render) · [referências](#referências)

## Links rápidos

| O quê | Link |
|---|---|
| Swagger UI (todos os endpoints, com exemplos de sucesso e de erro) | https://product-module-api.onrender.com/swagger-ui.html |
| API publicada | https://product-module-api.onrender.com/api/v1/produtos |
| Health check | https://product-module-api.onrender.com/actuator/health |
| Painel (Angular) que consome esta API | https://productmoduleinterface.vercel.app ([repositório](https://github.com/Chat809/product_module_interface)) |
| Execuções do CI | https://github.com/Chat809/product_module_api/actions |

> No plano gratuito do Render, a primeira requisição depois de um tempo sem uso pode levar até um minuto.

## Requisitos do teste técnico (PDF original)

O PDF com o enunciado não é versionado; a tabela segue o texto dele, sem alterá-lo.

| Requisito do PDF | Como foi cumprido | Onde está |
|---|---|---|
| API REST para gerenciar produtos (nome, descrição, preço, quantidade em estoque) | Recurso `/api/v1/produtos` com DTOs validados | `controller/ProdutoController`, `dto/ProdutoRequestDTO` |
| Criar produto | `POST /api/v1/produtos` → 201 com `Location` | `ProdutoController.criar` |
| Atualizar produto existente | `PUT /api/v1/produtos/{id}` → 200, ou 404 se não existir | `ProdutoController.atualizar` |
| Remover produto existente | `DELETE /api/v1/produtos/{id}` → 204 | `ProdutoController.remover` |
| Consultar todos e por id | `GET /api/v1/produtos` e `GET /api/v1/produtos/{id}` | `ProdutoController.listar` e `buscarPorId` |
| Boas práticas RESTful | Verbos e status do HTTP, sub-recursos, versionamento pela URL, erros RFC 9457 | [REST e SOLID](#rest-e-solid) |
| Spring Boot | Spring Boot 4.1.2 com Java 21 | `pom.xml` |
| Banco em memória H2 | Produtos e movimentações no H2 (`jdbc:h2:mem:produtosdb`) | `application.properties` |
| Padrão DAO | Interfaces sem tecnologia exposta e implementações com `EntityManager` | `dao/ProdutoDAO`, `dao/JpaProdutoDAO`, `dao/MovimentacaoDAO`, `dao/JpaMovimentacaoDAO` |
| Padrão Factory para os DAOs | Único ponto que conhece as implementações concretas | `factory/DAOFactory` |
| Padrão DTO | Records de entrada (com validação) e de saída; entidades nunca expostas | `dto/` |
| Testes unitários dos serviços | Serviços testados com DAOs falsos (Mockito) | `ProdutoServiceTest`, `MovimentacaoServiceTest` |
| Instruções para executar e testar | Esta seção e [Como executar](#como-executar) | README |

**Rodar e testar só o que foi pedido:**

```powershell
.\mvnw.cmd spring-boot:run     # Windows  (./mvnw spring-boot:run no Linux/macOS)
.\mvnw.cmd test -Dtest="ProdutoServiceTest,MovimentacaoServiceTest,ProdutoTest"
```

A API sobe em `http://localhost:8080` com 15 produtos de exemplo no H2. Os endpoints de produtos exigem login (extra 5); para testar no navegador, abra o [Swagger UI local](http://localhost:8080/swagger-ui.html), faça `POST /api/v1/auth/registro` e use o `accessToken` em **Authorize**.

## Funcionalidades extras

Cada extra tem o mesmo formato: o que faz, como foi construída e por que assim. Payloads completos e todos os erros estão no Swagger.

Produtos ficam no H2, como o PDF pede. Tudo o que precisa sobreviver a um reinício (usuários, perfis, demandas, contas e fotos) fica no **Supabase** (Auth, Postgres e Storage), escolha feita por você quando os módulos novos foram pedidos. A API acessa o Postgres pela API REST do Supabase (PostgREST) com a chave secreta, por trás de DAOs, como os produtos.

### 1. Paginação, busca e ordenação

**O que faz:** `GET /api/v1/produtos?busca=teclado&page=0&size=20&sort=preco,desc` devolve uma página com `conteudo`, `pagina`, `tamanho`, `totalElementos` e `totalPaginas`.

**Como foi construída:** `JpaProdutoDAO` monta a JPQL com `lower(...) like` para a busca e um `order by` escolhido de uma lista fechada de colunas (`nome`, `preco`, `quantidadeEmEstoque`, `criadoEm`, `atualizadoEm`). `size` vai de 1 a 500; valores fora disso respondem 400.

**Por que assim:** a lista fechada impede injeção pela URL. O limite de 500 deixa o painel carregar o catálogo inteiro uma vez e buscar no navegador, sem permitir respostas ilimitadas.

### 2. Edição concorrente com ETag

**O que faz:** impede que duas pessoas editando o mesmo produto sobrescrevam uma à outra.

**Como foi construída:** a entidade tem `@Version`. `GET /{id}` responde `ETag: "3"`; o `PUT` com `If-Match: "3"` só grava se a versão ainda for 3, senão responde 409. `If-Match: *` ou nenhum cabeçalho gravam sem checagem; um valor malformado responde 400. A versão também pode ir no corpo (`versao`).

**Por que assim:** é o mecanismo que o próprio HTTP define ([RFC 9110](https://www.rfc-editor.org/rfc/rfc9110#name-if-match)). Ser opcional não quebra clientes simples.

### 3. Movimentações de estoque

**O que faz:** registra entradas e saídas com motivo e mantém o histórico de cada produto.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/v1/produtos/{id}/movimentacoes` | `{ "tipo": "SAIDA", "quantidade": 5, "motivo": "Venda" }` → 201; saída maior que o estoque → 422 |
| `GET` | `/api/v1/produtos/{id}/movimentacoes` | Histórico do mais recente para o mais antigo, paginado |

**Como foi construída:** a regra de estoque não negativo fica na entidade (`registrarEntrada`, `registrarSaida`). Criar um produto registra o estoque inicial e editar a quantidade gera um `AJUSTE` automático.

**Por que assim:** movimentações são imutáveis; corrigir é registrar outra, o que mantém o histórico confiável.

### 4. Resumo para dashboard

**O que faz:** `GET /api/v1/produtos/resumo` devolve total de produtos e de itens, valor em estoque, esgotados e estoque baixo.

**Como foi construída:** consultas agregadas no `JpaProdutoDAO`; o limite de estoque baixo vem de `app.estoque.limite-baixo`.

**Por que assim:** os números saem prontos do banco, sem o cliente precisar baixar o catálogo.

### 5. Autenticação: e-mail e senha, e login com Google

**O que faz:** registro, login, login com Google e renovação de sessão. Todas as outras rotas exigem `Authorization: Bearer <accessToken>`.

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/api/v1/auth/registro` | Cria a conta (`nome`, `email`, `senha`) como `USUARIO` e abre a sessão → 201 |
| `POST` | `/api/v1/auth/login` | E-mail e senha → sessão |
| `POST` | `/api/v1/auth/google` | Troca `codigo` e `verificador` (PKCE) pela sessão |
| `POST` | `/api/v1/auth/refresh` | Troca o `refreshToken` por uma sessão nova |

**Como foi construída:** os usuários ficam no Supabase Auth. A API é um *resource server*: valida o JWT ES256 (assinatura pelo JWKS, emissor, audiência e expiração) sem consultar o Supabase a cada requisição. O Google segue o fluxo PKCE: o painel manda o desafio ao Supabase e a API troca código e verificador em `/auth/v1/token?grant_type=pkce`. Quem já tem conta com o mesmo e-mail entra nela, com o mesmo papel e acessos.

**Por que assim:** o painel não conhece nenhuma chave do Supabase, só a API conhece. PKCE foi pedido por você para que o token nunca apareça na URL. Configuração do Google: Client ID e Client Secret só no Supabase (*Authentication > Providers > Google*) e `<painel>/auth/callback` nas Redirect URLs.

### 6. Papéis e acessos por módulo

**O que faz:** cada usuário tem um nível de acesso, o campo `papel`, que só aceita dois valores: `ADMIN` (Administrador) ou `USUARIO` (Usuário). Também tem uma lista de módulos liberados: `dashboard`, `produtos`, `demandas`, `financeiro`, `usuarios` ou `["*"]` (acesso total). O que não foi liberado responde 403.

| Rota | Exige |
|---|---|
| `GET /produtos/resumo` | `dashboard` |
| `GET /produtos/**` | `dashboard` ou `produtos` |
| Escrita em `/produtos/**` | `produtos` |
| `/demandas/**` e `GET /usuarios/opcoes` | `demandas` |
| `/financeiro/**` | `financeiro` |
| `/usuarios/me/**` | qualquer usuário logado |
| Demais `/usuarios/**` | papel `ADMIN` |

**Como foi construída:** papel e acessos ficam em `public.perfis`. Um trigger copia os dois para o `app_metadata` do usuário, que vai no JWT e que ele não consegue alterar. O `PermissoesDoTokenConverter` transforma isso em `ROLE_*` e `MODULO_*`, exigidos rota a rota no `SecurityConfig`. Uma `check constraint` recusa valores desconhecidos. Conta nova recebe `["dashboard", "produtos", "demandas"]`.

**Por que assim:** `*` como acesso total foi pedido por você. Financeiro fica fora do padrão, também por escolha sua, e só entra quando o admin libera. `usuarios` é exclusivo de admin, e admin tem sempre `*`. Acessos novos valem no próximo login ou renovação (até 1 hora).

### 7. Gestão de usuários pelo admin

**O que faz:** o administrador lista as contas e edita nome, e-mail, departamento, cargo, nível de acesso, acessos, foto e senha de qualquer uma.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/usuarios?page=0&size=20` | Lista com departamento, cargo (`funcao`), nível de acesso (`papel`), acessos e foto |
| `PUT` | `/api/v1/usuarios/{id}` | `{ nome, email, papel, acessos, departamento, funcao }`; departamento e cargo até 80 caracteres, vazios viram `null`; e-mail de outra conta → 409; admin rebaixando a si mesmo → 422 |
| `PUT` | `/api/v1/usuarios/{id}/avatar` | Troca a foto da pessoa; mesmas regras de `/usuarios/me/avatar` |
| `DELETE` | `/api/v1/usuarios/{id}/avatar` | Remove a foto da pessoa |
| `PUT` | `/api/v1/usuarios/{id}/senha` | Define nova senha (6 a 72 caracteres) → 204 |

**Como foi construída:** `UsuarioService` grava nível, acessos, departamento e cargo em `perfis` (`SupabasePerfilDAO`) e só chama o Supabase Auth (`SupabaseAuthClient`) quando nome ou e-mail mudam. A troca de foto fica no `AvatarService`, usado tanto aqui quanto em "Meu perfil".

**Por que assim:** senhas não podem ser lidas por ninguém (o Supabase guarda só o hash), por isso existe "definir nova senha". E-mail, departamento e cargo são editáveis só pelo admin, como você pediu, porque descrevem a pessoa na empresa. O admin não pode tirar o próprio nível de administrador, para sempre restar um.

### 8. Meu perfil e avatar

**O que faz:** cada pessoa vê e edita os próprios dados (nome, telefone, bio) e envia uma foto. E-mail, departamento, cargo, nível de acesso e acessos aparecem só para leitura.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/usuarios/me` | Perfil completo, com departamento, cargo, nível e acessos atuais do banco |
| `PUT` | `/api/v1/usuarios/me` | `{ nome, telefone, bio }` |
| `PUT` | `/api/v1/usuarios/me/avatar` | Multipart, campo `arquivo`: PNG, JPEG ou WebP até 2 MB; maior → 413 |
| `DELETE` | `/api/v1/usuarios/me/avatar` | Remove a foto |
| `GET` | `/api/v1/usuarios/opcoes` | Só id, nome e foto de todos, para escolher responsáveis (módulo `demandas`) |

**Como foi construída:** as colunas novas ficam em `perfis`; o nome também é atualizado no Supabase Auth. A foto vai para o bucket público `avatars` do Supabase Storage pelo `SupabaseStorageClient`, em `{id}/{uuid}.{ext}`, e a foto anterior é apagada. O tipo é detectado pelos primeiros bytes do arquivo (`FormatoDeImagem`), não pelo cabeçalho enviado.

**Por que assim:** o nome do arquivo é um UUID, que não expõe nome nem e-mail. Validar pelo conteúdo impede renomear um arquivo qualquer para `.png`. O bucket só permite leitura pública; quem grava é a API, com a chave secreta.

### 9. Demandas e tarefas

**O que faz:** quadro Kanban com as colunas `A_FAZER`, `EM_ANDAMENTO`, `EM_REVISAO` e `CONCLUIDO`. Cada demanda tem título, descrição, tipo, urgência, prazo, responsáveis e checklist. As demandas abertas em que a pessoa é responsável são as "Minhas tarefas" do perfil.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/demandas?responsavel=me&abertas=true&status=&busca=` | Lista na ordem do quadro (até 500); `atrasada` vem calculada |
| `GET` | `/api/v1/demandas/{id}` | Uma demanda |
| `POST` | `/api/v1/demandas` | Cria no fim da coluna (padrão `A_FAZER`) → 201 |
| `PUT` | `/api/v1/demandas/{id}` | Edita |
| `PATCH` | `/api/v1/demandas/{id}/status` | `{ "status": "EM_REVISAO", "posicao": 0 }`: move o cartão |
| `DELETE` | `/api/v1/demandas/{id}` | Só quem criou ou um admin; outro usuário → 422 |

**Como foi construída:** tabela `demandas` com `responsaveis uuid[]` (índice GIN), `checklist jsonb` e `ordem`. Um trigger põe a demanda nova no fim da coluna, e a função `mover_demanda` renumera a coluna de destino numa única transação. Responsáveis são conferidos contra os perfis existentes. "Hoje" usa o fuso de Brasília.

**Por que assim:** usar as próprias demandas como tarefas, em vez de um cadastro separado, foi decisão sua. Reordenar no banco evita posições repetidas quando duas pessoas arrastam ao mesmo tempo.

### 10. Financeiro: contas a pagar e a receber

**O que faz:** lançamento de contas a pagar (com favorecido) e a receber (com pagador), com percentual de imposto, forma de pagamento e chave Pix, baixa com data, cancelamento e um resumo.

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/v1/financeiro/resumo` | A pagar, a receber, vencidos, pago e recebido no mês, saldo previsto (pelo líquido) |
| `GET` | `/api/v1/financeiro/{tipo}?status=&vencidas=&busca=&vencimentoDe=&vencimentoAte=&page=&size=` | Lista paginada em ordem de vencimento |
| `POST`, `PUT` | `/api/v1/financeiro/{tipo}` e `/{tipo}/{id}` | Lança ou edita; Pix sem chave → 400 |
| `PATCH` | `/api/v1/financeiro/{tipo}/{id}/baixa` | `{ "data": "2026-11-03" }` (padrão hoje, nunca futura); já quitada → 422 |
| `PATCH` | `/api/v1/financeiro/{tipo}/{id}/cancelamento` | Cancela uma pendente, mantendo o histórico |
| `DELETE` | `/api/v1/financeiro/{tipo}/{id}` | Exclui lançamento feito por engano |

`{tipo}` é `contas-pagar` ou `contas-receber`.

**Como foi construída:** tabelas `contas_pagar` e `contas_receber` com o mesmo formato. `valor_imposto` e `valor_liquido` são colunas geradas pelo banco (`round(valor × percentual / 100, 2)`). Constraints garantem chave Pix quando a forma é PIX e data de baixa se, e somente se, a conta estiver quitada. O resumo é a função `resumo_financeiro`.

**Por que assim:** imposto como percentual, com o líquido calculado no banco, foi o que você pediu; assim nenhum cliente consegue gravar um líquido inconsistente. Os totais usam o líquido, que é o que de fato entra ou sai do caixa. Exige o módulo `financeiro`, que contas novas não recebem.

## Referência técnica

### Stack

- Java 21 e Spring Boot 4.1.2 (Web MVC, Data JPA, Bean Validation, Actuator, Security como OAuth2 Resource Server)
- H2 em memória para produtos e movimentações
- Supabase: Auth (usuários), Postgres via PostgREST (perfis, demandas, contas) e Storage (fotos)
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, AssertJ e JaCoCo; Maven Wrapper

### Como executar

Pré-requisito: JDK 21 (o Maven não precisa estar instalado).

```bash
git clone https://github.com/Chat809/product_module_api.git
cd product_module_api
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

Para o login e os módulos guardados no Supabase, defina antes a chave secreta (em *Project Settings > API Keys*), que nunca vai para o repositório:

```powershell
$env:SUPABASE_SECRET_KEY = "<SUPABASE_SECRET_KEY>"    # Linux/macOS: export SUPABASE_SECRET_KEY="<SUPABASE_SECRET_KEY>"
```

Sem a chave, a aplicação sobe, mas login e os módulos do Supabase respondem 503 explicando o que falta.

**Testar os produtos localmente sem a chave:** a API local confere o token pelas chaves públicas do Supabase, então um token emitido pela API publicada também vale aqui. Os produtos continuam no H2 da sua máquina.

1. Faça login na API publicada pelo [Swagger do Render](https://product-module-api.onrender.com/swagger-ui.html) (`POST /api/v1/auth/login` com seu e-mail e senha) e copie o `accessToken` da resposta. Ele vale por 1 hora.
2. Abra o [Swagger local](http://localhost:8080/swagger-ui.html), clique em **Authorize**, cole o token e use os endpoints de `/api/v1/produtos`.

| Recurso | URL |
|---|---|
| API | http://localhost:8080/api/v1/produtos |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Console H2 (só no perfil `dev`) | http://localhost:8080/h2-console (JDBC `jdbc:h2:mem:produtosdb`, usuário `sa`, senha em branco) |

### Testes

```bash
./mvnw clean verify             # Windows: .\mvnw.cmd clean verify
```

São 239 testes. O build gera `target/site/jacoco/index.html` e **falha se a cobertura de linhas ficar abaixo de 80%** (hoje em 98%). Nenhum teste acessa o Supabase: as chamadas HTTP são simuladas com `MockRestServiceServer`.

| Classe de teste | O que cobre |
|---|---|
| `ProdutoTest` | Regras da entidade: normalização, entrada, saída e estoque insuficiente (sem Spring) |
| `ProdutoServiceTest`, `MovimentacaoServiceTest` | CRUD, paginação, conflito de versão, ajuste automático, resumo e histórico (Mockito) |
| `ProdutoControllerTest`, `MovimentacaoControllerTest` | Status, `Location`, `ETag`/`If-Match`, validação, CORS, 401 e 403 por módulo (`@WebMvcTest`) |
| `JpaProdutoDAOTest`, `JpaMovimentacaoDAOTest` | Consultas JPQL, auditoria, versão e agregados no H2 real (`@DataJpaTest`) |
| `AuthControllerTest`, `AuthServiceTest`, `SupabaseAuthClientTest` | Registro, login, Google, refresh; corpo enviado ao Supabase e tradução de cada erro |
| `FalhaDoSupabaseTest` | Falhas do Supabase registradas no log só pelo status ou tipo, sem corpo nem URL |
| `UsuarioControllerTest`, `UsuarioServiceTest`, `SupabasePerfilDAOTest` | Lista, edição com departamento e cargo, foto pelo admin, nova senha, `/opcoes`; PostgREST, `Content-Range` e colunas do perfil |
| `PermissoesDoTokenConverterTest`, `AcessosTest` | `ROLE_*` e `MODULO_*` a partir do token; forma canônica dos acessos e padrão de conta nova |
| `MeuPerfilControllerTest`, `MeuPerfilServiceTest`, `AvatarServiceTest`, `SupabaseStorageClientTest`, `FormatoDeImagemTest` | `/usuarios/me`, multipart, 413, imagem falsa recusada, troca e remoção da foto no Storage |
| `DemandaControllerTest`, `DemandaServiceTest`, `SupabaseDemandaDAOTest` | Filtros, `responsavel=me`, criação no fim da coluna, mover pela função, exclusão só do autor ou admin |
| `FinanceiroControllerTest`, `ContaServiceTest`, `SupabaseContaDAOTest` | Os dois tipos de conta, regra do Pix, baixa sem data futura, conta já quitada, filtros, paginação e resumo |
| `GlobalExceptionHandlerTest`, `ProdutosApiApplicationTests` | Formato RFC 9457 de cada erro sem vazar detalhes; subida do contexto |

### Formato dos erros

Todos os erros seguem a [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) com `Content-Type: application/problem+json`. O Swagger mostra o exemplo de cada um.

```json
{
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos são inválidos",
  "instance": "/api/v1/produtos",
  "erros": [{ "campo": "nome", "mensagem": "O nome é obrigatório" }]
}
```

Outros status: 401 (sem token), 403 (sem o módulo ou papel), 404, 409 (edição concorrente ou e-mail repetido), 413 (arquivo grande), 422 (regra de negócio, como estoque insuficiente) e 503 (Supabase indisponível ou chave ausente). Mensagens do Supabase nunca são repassadas cruas.

### Arquitetura

```
br.com.triboraizes.produtosapi
├── config        OpenAPI, CORS, segurança, relógio (fuso de Brasília), Supabase e dados iniciais
├── controller    Endpoints REST (só HTTP, sem regra de negócio)
├── service       Regras de negócio e transações
├── dao           Contratos e implementações: JPA (produtos) e Supabase (perfis, demandas, contas)
├── factory       DAOFactory: criação dos DAOs JPA
├── client        SupabaseAuthClient e SupabaseStorageClient
├── security      Papel e acessos do JWT em permissões do Spring Security
├── dto           Entrada e saída da API
├── model         Entidades, enums e objetos de valor
└── exception     Exceções de domínio e tratamento global de erros
```

- **DAO:** interfaces sem tecnologia exposta. `Jpa*DAO` usam o `EntityManager`; `Supabase*DAO` implementam os mesmos contratos sobre o PostgREST, e os serviços não sabem a diferença.
- **Factory:** `DAOFactory` é o único ponto que conhece as implementações JPA; trocar a persistência mexe só nela.
- **DTO:** records de entrada com validação e de saída; o cliente não define `id`, datas, saldo nem valor líquido.

### REST e SOLID

| REST | Como foi aplicado |
|---|---|
| Recursos e sub-recursos | `/produtos/{id}/movimentacoes`, `/usuarios/me/avatar`, `/demandas/{id}/status`, `/financeiro/contas-pagar/{id}/baixa` |
| Verbos com semântica | `GET` sem efeito; `POST` cria; `PUT` substitui; `PATCH` muda um aspecto (status, baixa); `DELETE` remove |
| Status corretos | 200, 201 com `Location`, 204, 400, 401, 403, 404, 409, 413, 422, 503 |
| Concorrência | `ETag` e `If-Match` |
| Sem estado e versionada | JWT em cada requisição; `/api/v1` |

| SOLID | Onde aparece |
|---|---|
| Responsabilidade única | Controller traduz HTTP, service aplica regras, DAO persiste, `GlobalExceptionHandler` formata erros |
| Aberto/fechado | Nova persistência é nova implementação do DAO; novo erro é nova exceção com handler |
| Liskov | Testes de serviço trocam os DAOs por falsos sem mudar o comportamento |
| Segregação de interfaces | Um DAO por agregado, só com o que os serviços usam |
| Inversão de dependência | Serviços dependem das interfaces e recebem tudo pelo construtor |

### Segurança

**Onde fica cada segredo**

| Segredo | Onde fica |
|---|---|
| `SUPABASE_SECRET_KEY` | Só nas variáveis de ambiente do Render e da máquina local |
| Client ID e Client Secret do Google | Só no Supabase |
| Senhas dos usuários | Só como hash no Supabase Auth |

Variáveis de ambiente: `SUPABASE_SECRET_KEY` (obrigatória para login e módulos do Supabase), `SUPABASE_URL` (tem padrão), `CORS_ALLOWED_ORIGINS`, `DADOS_INICIAIS` e `PORT`. O painel conhece só a URL pública do Supabase.

**Proteções**

- JWT ES256 validado localmente; papel e acessos vêm do `app_metadata`, que só a chave secreta altera.
- Tabelas do Supabase com RLS ligado e sem políticas: nada é acessível com a chave pública, só pela API. Funções com `search_path` fixo e `execute` revogado de `anon` e `authenticated`.
- Bucket `avatars`: leitura pública só dos arquivos, sem listagem nem escrita pública; nome em UUID; tipo validado pelo conteúdo; 2 MB.
- Dados mínimos: `/usuarios/opcoes` devolve só id, nome e foto; contas e chaves Pix só com o módulo `financeiro`.
- Logs registram id ou e-mail, nunca tokens, código do Google, verificador PKCE, chave ou corpo de resposta do Supabase.
- Bean Validation em todos os campos, JPQL parametrizada e ordenação por lista fechada.
- CORS restrito às origens do painel; erros sem stack trace; console H2 só em `dev`; Actuator só com `/health`; container sem root; ids UUID.

**O que não existe, e por quê:** não há rate limit próprio (o Supabase Auth já limita tentativas de login), revogação imediata de token (expira em 1 hora) nem cofre de segredos (uma única chave no Render). Em produção, entrariam Bucket4j ou gateway, tokens mais curtos e um gerenciador de segredos com rotação.

### Deploy no Render

O `Dockerfile` multi-stage compila com JDK 21 e roda com JRE 21 Alpine, usuário não-root e perfil `prod`.

1. Crie um **Web Service** com runtime **Docker** a partir do repositório.
2. **Health Check Path:** `/actuator/health`.
3. **Environment:** `SUPABASE_SECRET_KEY`. Opcionais: `CORS_ALLOWED_ORIGINS` (substitui a lista padrão) e `DADOS_INICIAIS=false`.
4. A cada push, faça **Manual Deploy > Deploy latest commit** (o auto-deploy está desligado).

No plano gratuito, o serviço dorme após uns 15 minutos sem uso, e os produtos (H2) voltam ao estado inicial a cada reinício; os dados do Supabase permanecem.

```bash
docker build -t produtos-api .
docker run -p 8080:8080 -e SUPABASE_SECRET_KEY="<SUPABASE_SECRET_KEY>" produtos-api
```

### Referências

- [Spring Boot](https://docs.spring.io/spring-boot/index.html), [Spring Web MVC](https://docs.spring.io/spring-framework/reference/web/webmvc.html) e [ProblemDetail](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [Spring Security: Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Jakarta Persistence](https://jakarta.ee/specifications/persistence/), [Hibernate](https://hibernate.org/orm/documentation/), [Bean Validation](https://beanvalidation.org/) e [H2](https://www.h2database.com/)
- [springdoc-openapi](https://springdoc.org/)
- [JUnit 5](https://junit.org/junit5/docs/current/user-guide/), [Mockito](https://site.mockito.org/), [AssertJ](https://assertj.github.io/doc/) e [JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/)
- [Supabase Auth](https://supabase.com/docs/guides/auth), [login com Google](https://supabase.com/docs/guides/auth/social-login/auth-google), [PKCE](https://supabase.com/docs/guides/auth/sessions/pkce-flow), [PostgREST](https://supabase.com/docs/guides/api), [Storage](https://supabase.com/docs/guides/storage) e [RLS](https://supabase.com/docs/guides/database/postgres/row-level-security)
- [RFC 7636 (PKCE)](https://www.rfc-editor.org/rfc/rfc7636), [RFC 9110 (ETag e If-Match)](https://www.rfc-editor.org/rfc/rfc9110) e [RFC 9457 (Problem Details)](https://www.rfc-editor.org/rfc/rfc9457)
- [Render: Docker](https://render.com/docs/docker) e [GitHub Actions](https://docs.github.com/actions)
