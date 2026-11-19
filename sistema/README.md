# Sistema de Simulação de Eleição Eletrônica (Back-End)

API REST em Java com Spring Boot e MySQL.

## Tecnologias
Java 17, Spring Boot, Spring Data JPA, Spring Security + JWT, Bean Validation, MySQL, springdoc (Swagger UI), JUnit 5 e H2 (testes).

## Como rodar
1. Ter o JDK 17 (ou superior) e o MySQL ligados.
2. Definir as variáveis de ambiente:
   - `DB_USER` (padrão: `root`)
   - `DB_PASSWORD`
3. Na raiz do projeto: `./mvnw spring-boot:run`
4. Swagger UI: http://localhost:8080/swagger-ui.html

## Testes
`./mvnw test`

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
