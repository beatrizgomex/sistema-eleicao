# Sistema de Simulação de Eleição Eletrônica (Back-End)

API REST em Java com Spring Boot e MySQL.

## Tecnologias
Java 17, Spring Boot, Spring Data JPA, Spring Security + JWT, Bean Validation, MySQL, springdoc (Swagger UI), JUnit 5 e H2 (testes).

## Como rodar
1. Ter o JDK 17 (ou superior) e o MySQL ligados.
2. Definir as variáveis de ambiente:
   - `DB_USER` (padrão: `root`)
   - `DB_PASSWORD`
   - `JWT_SECRET` com uma chave aleatória de pelo menos 32 bytes para emitir tokens.
   - `CHEFE_SENHA` na primeira inicialização de um banco sem Chefe. O login inicial é `chefe` e pode ser configurado em `CHEFE_LOGIN`.
3. Na raiz do projeto: `./mvnw spring-boot:run`
4. Swagger UI: http://localhost:8080/swagger-ui.html

## Testes
`./mvnw test`

### Módulo autenticação

A rota `POST /auth/login` recebe `login` e `senha` em JSON. A API busca o usuário pelo login e verifica a senha usando o hash armazenado no banco. Quando as credenciais são válidas, retorna `200` com `token`, `tipo` (`Bearer`), `expiraEm` e `perfil`. Campos obrigatórios ausentes ou em branco retornam `400`. Credenciais inválidas retornam `401`.

O JWT é assinado com HS256 e expira em 60 minutos na configuração padrão. Ele contém o ID, o login e o perfil do usuário autenticado.

#### Organização do módulo

- `ControladorAutenticacao` recebe as requisições de login.
- `ServicoAutenticacao` verifica as credenciais e monta a resposta.
- `ServicoJwt` emite os tokens e verifica a assinatura e a expiração na validação.
- `ConfiguracaoChefeInicial` cria a conta inicial na primeira execução em que o banco ainda não tem um usuário com perfil `CHEFE`.

#### Dados de autenticação

A entidade `Usuario` é persistida na tabela `usuario`, que armazena o ID, o login único, o hash da senha e o perfil de acesso. O Chefe da Seção usa essa mesma tabela, com o perfil `CHEFE`.

### Módulos `eleitor` e `candidato`

Cadastros que alimentam a eleição: eleitores, candidatos e partidos. O acesso é restrito ao **Chefe da Seção**. A consulta de candidatos por cargo também é permitida ao **Eleitor**.

#### Eleitores

| Método | Rota | Função |
| --- | --- | --- |
| POST | `/eleitores` | Cadastra o eleitor e o seu usuário de acesso |
| GET | `/eleitores` | Lista os eleitores |
| GET | `/eleitores/{id}` | Consulta um eleitor |
| PUT | `/eleitores/{id}` | Altera nome e documento |
| DELETE | `/eleitores/{id}` | Exclui o eleitor e o seu usuário |

#### Candidatos

| Método | Rota | Função |
| --- | --- | --- |
| POST | `/candidatos` | Cadastra o candidato com cargo, partido, eleição e usuário de acesso |
| GET | `/candidatos` | Lista os candidatos |
| GET | `/candidatos?eleicaoId=&cargo=` | Lista os candidatos disponíveis para votação em um cargo |
| GET | `/candidatos/{id}` | Consulta um candidato |
| PUT | `/candidatos/{id}` | Altera nome e partido |
| DELETE | `/candidatos/{id}` | Exclui o candidato e o seu usuário |

#### Partidos

CRUD em `/partidos` (`POST`, `GET`, `PUT`, `DELETE`), necessário para associar os candidatos.

#### Regras

- Documento do eleitor, sigla do partido e login são únicos (`409` se repetir).
- O número do candidato é único por cargo e eleição (`409` se repetir).
- Partido ou eleição inexistente no cadastro do candidato retorna `404`.
- Campos obrigatórios vazios retornam `400`.
- Cada cadastro cria também o `Usuario` de acesso (`ELEITOR` ou `CANDIDATO`), com senha em BCrypt. As respostas nunca devolvem a senha.
- Excluir um eleitor ou candidato remove também o seu usuário.

Exemplos de requisição e resposta estão no Swagger UI.

### Módulo `votacao` (urna)

Registro dos votos e comprovante de votação. O voto é feito pelo **Eleitor**.

| Método | Rota | Função |
| --- | --- | --- |
| GET | `/urna/cedula?eleicaoId=` | Lista os cargos em disputa e os candidatos de cada um |
| POST | `/urna/votos` | Registra a cédula do eleitor logado e devolve o comprovante |
| GET | `/urna/comprovante?eleicaoId=` | Devolve o comprovante do eleitor logado |

Exemplo de cédula, com um voto por cargo em disputa:

```json
{
  "eleicaoId": 1,
  "votos": [
    { "cargo": "GOVERNADOR", "tipo": "CANDIDATO", "numero": 10 },
    { "cargo": "PRESIDENTE", "tipo": "BRANCO" }
  ]
}
```

Resposta (`201`):

```json
{ "codigo": "4F7A-9C21-B03E-77D5", "eleicaoId": 1, "emitidoEm": "2026-11-20T10:15:00" }
```

No perfil `dev`, o eleitor logado vem dos cabeçalhos `X-Usuario-Id` (id do usuário) e `X-Perfil` (`ELEITOR`).

#### Regras

- Só vota quem tem perfil `ELEITOR` (`401` sem login, `403` com outro perfil).
- A eleição precisa estar `ABERTA` (`409` se não estiver).
- Cada eleitor vota uma vez por eleição (`409` no segundo voto).
- A cédula precisa de exatamente um voto para cada cargo em disputa; faltar cargo, repetir cargo ou votar em cargo sem disputa retorna `400`.
- `CANDIDATO` exige o número, e `BRANCO` e `NULO` não levam número (`400`). Número sem candidato no cargo retorna `404`.

#### Sigilo e integridade

- O `Voto` não guarda o eleitor nem o horário, só eleição, cargo, tipo e candidato.
- Quem votou fica no `ComprovanteVotacao`, sem dizer em quem.
- O id do voto é UUID aleatório, para a ordem de gravação não ligar o voto ao comprovante.
- Votos e comprovante são gravados na mesma transação.
- Restrição única de eleitor e eleição no comprovante, para barrar dois envios ao mesmo tempo.
- A eleição é lida com trava durante o voto, então o encerramento espera os votos em andamento.
