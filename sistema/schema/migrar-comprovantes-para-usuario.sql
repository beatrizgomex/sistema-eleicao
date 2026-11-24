-- MySQL: executar uma vez, com a aplicação parada, sobre o esquema anterior.
-- A troca de nomes é atômica e mantém a tabela original como cópia.
CREATE TABLE comprovante_por_usuario (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    eleicao_id BIGINT NOT NULL,
    codigo VARCHAR(19) NOT NULL UNIQUE,
    emitido_em DATETIME(6) NOT NULL,
    UNIQUE (usuario_id, eleicao_id),
    FOREIGN KEY (usuario_id) REFERENCES usuario(id),
    FOREIGN KEY (eleicao_id) REFERENCES eleicao(id)
);

INSERT INTO comprovante_por_usuario (id, usuario_id, eleicao_id, codigo, emitido_em)
SELECT c.id, e.usuario_id, c.eleicao_id, c.codigo, c.emitido_em
FROM comprovante_votacao c
LEFT JOIN eleitor e ON e.id = c.eleitor_id;

RENAME TABLE comprovante_votacao TO comprovante_votacao_anterior,
             comprovante_por_usuario TO comprovante_votacao;
