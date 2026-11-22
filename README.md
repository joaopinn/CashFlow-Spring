# CashFlow

## Banco de dados

Execute os comandos a partir da pasta `demo` (também configure essa pasta como working directory na IDE).

A configuração local fica em `config/database-local.properties`, ignorado pelo Git.
Para configurar outra máquina, copie `config/database-local.properties.example`
para `config/database-local.properties` e preencha as credenciais.

Em ambientes de implantação, configure `DATABASE_URL` (URL JDBC),
`DATABASE_USERNAME` e `DATABASE_PASSWORD` como variáveis de ambiente.
O driver JDBC usa `channelBinding=require`, com B maiúsculo.

```sh
./mvnw spring-boot:run
```

O Hibernate apenas valida o esquema, sem criar ou alterar tabelas.
O Flyway está desabilitado até a criação da primeira migration; depois,
habilite com `FLYWAY_ENABLED=true`.

## Autenticação e execução

Java 21 ou superior; Maven Wrapper incluído. Antes de iniciar, configure o banco acima e um segredo JWT:

```sh
export JWT_SECRET="$(openssl rand -base64 32)"
./mvnw spring-boot:run
```

Guarde o segredo no gerenciador de segredos do ambiente. Use o mesmo valor nas instâncias da aplicação;
sem ele, ou com menos de 32 bytes decodificados, a aplicação falha na inicialização.
Não gere uma chave nova em cada inicialização: isso invalida os tokens já emitidos.

| Variável | Padrão | Uso |
|---|---|---|
| `JWT_SECRET` | obrigatório | Chave HS256, Base64 de pelo menos 32 bytes aleatórios |
| `JWT_ISSUER` | `cashflow` | Emissor aceito e emitido |
| `JWT_AUDIENCE` | `cashflow-api` | Audiência aceita e emitida |
| `JWT_TTL_SECONDS` | `900` | Duração entre 60 e 86400 segundos |
| `SWAGGER_ENABLED` | `true` | Habilita OpenAPI e Swagger UI |

### Banco existente e primeiro administrador

Antes de publicar, revise e aplique `db/production-preflight.sql` no PostgreSQL. Ele rejeita e-mails
vazios/duplicados, normaliza os e-mails, cria um índice único e índices para filtros por usuário,
período e tipo. Converte perfis antigos diferentes de `ADMIN` em `USER`.
A restrição única no banco é necessária para proteger cadastros simultâneos; a checagem em Java
sozinha não impede concorrência. O script pressupõe que as tabelas das entidades já existem.
O Hibernate permanece em `validate`; testes H2 criam seu próprio esquema. Não habilite Flyway sem
antes preparar e revisar uma baseline e as migrations do esquema existente.

O cadastro público nunca cria ADMIN. Cadastre a primeira conta e atribua `ADMIN` a ela por um canal
administrativo confiável no banco, usando seu ID, por exemplo:

```sql
UPDATE users SET role = 'ADMIN' WHERE id_user = <ID_DA_CONTA_ADMINISTRATIVA>;
```

Senhas existentes precisam estar em BCrypt. Não há senha administrativa nem segredo embutidos.
`role` agora representa autorização (`USER` ou `ADMIN`), não cargo comercial.

## Swagger / OpenAPI

Com a aplicação local em execução:

- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Todas as operações têm resumo, permissões, modelos de entrada/saída, validação e respostas de erro.
O botão **Authorize** recebe somente o token JWT; a interface acrescenta `Bearer`.
Login e cadastro são públicos. A autorização não é persistida pelo Swagger no navegador.
Use `SWAGGER_ENABLED=false` para desabilitar a documentação em um ambiente.

### Cadastro, login e autorização

```http
POST /auth/register
Content-Type: application/json

{
  "name": "Maria",
  "cnpj": "11222333000181",
  "segment": "Varejo",
  "email": "maria@example.com",
  "password": "senha-segura-123",
  "phone": "71999999999"
}
```

Retorna `201`, `Location: /users/{id}` e os dados sem senha. Campos desconhecidos, incluindo `role`,
são rejeitados. A senha deve ter 12 a 72 caracteres e no máximo 72 bytes UTF-8 (limite BCrypt).
CNPJ precisa ter dígitos verificadores válidos; e-mail é armazenado em minúsculas e deve ser único.

```http
POST /auth/login
Content-Type: application/json

{"email":"maria@example.com","password":"senha-segura-123"}
```

```json
{"accessToken":"<JWT>","tokenType":"Bearer","expiresIn":900,"userId":1}
```

Nas demais requisições: `Authorization: Bearer <JWT>`. Login retorna `Cache-Control: no-store`.
Senha incorreta e e-mail inexistente produzem a mesma mensagem. A autenticação é stateless, sem sessão
ou cookie de autenticação; por isso CSRF está desabilitado. CORS não libera origens por padrão.
Use HTTPS e configure limites de requisições, especialmente no login/cadastro, no proxy de produção.

O servidor confere assinatura HS256, emissor, audiência, expiração, início de validade e ID de usuário.
O validador de tempo do Spring Security admite sua tolerância padrão de relógio (60 segundos).
Tokens de contas excluídas deixam de funcionar e o perfil é relido do banco a cada requisição.
Não há refresh token nem endpoint de logout/revogação individual: o cliente descarta o token no logout;
sem excluir a conta ou trocar a chave, ele vale até expirar. A troca de senha não revoga tokens existentes.
A chave deve ser rotacionada de forma coordenada; esta versão aceita uma única chave por vez.

| Recurso | USER | ADMIN |
|---|---|---|
| `POST /auth/register`, `POST /auth/login` | Público | Público |
| `GET /users`, `GET /users/search`, `POST /users` | Negado | Permitido |
| `GET/PUT/DELETE /users/{id}` | Apenas seu ID; não pode mudar o perfil | Permitido |
| Categorias e transações em `/users/{userId}/...` | Apenas seu ID | Permitido, mantendo o escopo da rota |
| `GET /transaction-types`, `GET /transaction-types/{id}` | Permitido | Permitido |
| `POST/PUT/DELETE /transaction-types[/{id}]` | Negado | Permitido |

O PUT é uma substituição completa e exige todos os campos do DTO. O ID vem da rota e o registro
existente é atualizado, preservando o proprietário. No PUT de usuário, mantenha `role: "USER"`
para uma conta comum; somente ADMIN altera perfis. As respostas nunca retornam a senha ou hash.

## Rotas e filtros

| Recurso | Listar | Busca | Criar | Consultar / atualizar / excluir |
|---|---|---|---|---|
| Usuários | `GET /users` | `GET /users/search` | `POST /users` (ADMIN) | `GET/PUT/DELETE /users/{id}` |
| Categorias | `GET /users/{userId}/categories` | `GET /users/{userId}/categories/search` | `POST /users/{userId}/categories` | `GET/PUT/DELETE /users/{userId}/categories/{id}` |
| Transações | `GET /users/{userId}/transactions` | `GET /users/{userId}/transactions/search` | `POST /users/{userId}/transactions` | `GET/PUT/DELETE /users/{userId}/transactions/{id}` |
| Tipos | `GET /transaction-types` | — | `POST /transaction-types` | `GET/PUT/DELETE /transaction-types/{id}` |

Os aliases antigos de transações `/getAllTransaction`, `/create` e `/delete/{id}` continuam funcionando.
Categorias ainda aceitam `GET /users/{userId}/categories?name=...` como alias da busca.

Todos os filtros são opcionais e combinados com AND; sem filtros a busca lista todos no escopo autorizado.
Busca textual parcial ignora maiúsculas; `%` e `_` são literais, não curingas.

| Busca | Parâmetros |
|---|---|
| Usuários | `name`, `email`, `segment` (parciais), `role` (exato, ignora caixa) |
| Categorias | `name` (título parcial), `typeId` (ID positivo) |
| Transações | `name` (descrição parcial), `typeId`, `startDate`, `endDate` |

Datas: `yyyy-MM-dd`, com início e fim inclusivos. Cada limite pode ser usado isoladamente.
Início depois do fim retorna `400`. Tipo positivo inexistente simplesmente não encontra resultados.
Criação/alteração exige categoria do usuário e tipo igual ao da categoria.
Alterar o tipo de uma categoria que já possui transações incompatíveis retorna `409`.
Valor monetário deve ser positivo, ter até 17 dígitos inteiros e duas casas decimais.
`status` é texto obrigatório de até 50 caracteres, sem catálogo fechado nesta versão.

```http
GET /users/1/transactions/search?name=mercado&typeId=2&startDate=2026-11-01&endDate=2026-11-30&page=0&size=20&sort=date,desc
Authorization: Bearer <JWT>
```

Paginação: `page` começa em 0, `size` entre 1 e 100 (padrão 20), `sort=campo,asc|desc`.
A resposta contém `content`, `totalElements`, `totalPages`, `number` e `size`.
Ordenação padrão: usuário por `name`, categoria/tipo por `title`, transação por `date`.
Campos permitidos para sort: usuários `id,name,email,segment,role`; categorias/tipos `id,title`;
transações `id,date,amount,description,status`. Campos internos e relacionamentos não são aceitos.

## Erros padronizados

`@RestControllerAdvice` trata erros de validação e da aplicação. Os handlers do Spring Security
usam o mesmo JSON para falhas ocorridas antes do controller:

```json
{
  "timestamp": "2026-11-16T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Dados inválidos",
  "path": "/auth/register",
  "fieldErrors": [{"field": "email", "message": "deve ser um endereço de e-mail bem formado"}]
}
```

`fieldErrors` é uma lista vazia quando o erro não é de validação por campo. As mensagens de Bean
Validation dependem do locale; os clientes devem usar status/campo, não comparar a tradução.
Valores rejeitados, senhas, tokens, SQL e stack traces não entram na resposta.

| Status | Significado |
|---|---|
| 400 | DTO inválido, JSON malformado, campo desconhecido, data/ID/filtros/paginação inválidos |
| 401 | Credenciais incorretas, token ausente, inválido, expirado ou conta removida |
| 403 | Acesso a outro usuário, ação administrativa proibida ou tentativa de promoção |
| 404 | Recurso não encontrado no escopo solicitado |
| 405 / 415 | Método HTTP / Content-Type não suportado |
| 409 | E-mail duplicado ou exclusão bloqueada por vínculos |
| 500 | Falha inesperada; detalhes ficam somente nos logs do servidor |

## Testes

```sh
./mvnw test
```

O perfil `test` usa H2 em modo PostgreSQL com esquema descartável e chave JWT exclusiva de testes.
Não lê as credenciais locais nem acessa o Neon. Inclui testes unitários e integração
`@SpringBootTest` + `@AutoConfigureMockMvc`, com controllers, advice, filtros de segurança, JWT real,
services, repositories, Bean Validation e SQL reais; não há mocks de services ou de persistência
nos testes de integração. Os testes verificam autenticação, perfis, isolamento de usuários,
respostas padrão, unicidade, CRUD, filtros de período/tipo/texto, paginação e o OpenAPI gerado.

H2 não substitui a validação de migrations e comportamento específico do PostgreSQL. Antes de publicar,
execute o script revisado e uma verificação de implantação contra uma instância de homologação PostgreSQL.
Nenhum teste desta suíte modifica o banco de produção.

Referências: [Spring Security JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
e [springdoc para Spring Boot 4](https://springdoc.org/).
