-- MySQL: executar uma vez, caso a tabela usuario já exista sem as colunas de controle de tentativas de login.
-- Em bancos novos (ou com spring.jpa.hibernate.ddl-auto=update), o Hibernate cria as colunas automaticamente.
ALTER TABLE usuario
    ADD COLUMN tentativas_falhas INT NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_ate DATETIME(6) NULL;
