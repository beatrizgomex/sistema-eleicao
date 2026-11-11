# Sistema de Simulação de Eleição Eletrônica (Back-End)

API REST em Java com Spring Boot e MySQL.

## Tecnologias
Java 17, Spring Boot, Spring Data JPA, Spring Security + JWT, Bean Validation, MySQL, springdoc (Swagger UI), JUnit 5 e H2 (testes).

## Como rodar
1. Ter o JDK 17 (ou superior) e o MySQL ligados.
2. Definir as variáveis de ambiente (se o seu MySQL não usar `root` sem senha):
   - `DB_USER` (padrão: `root`)
   - `DB_PASSWORD`
3. Na raiz do projeto: `./mvnw spring-boot:run`
4. Swagger UI: http://localhost:8080/swagger-ui.html

## Testes
`./mvnw test`

## Integrantes
(preencher)