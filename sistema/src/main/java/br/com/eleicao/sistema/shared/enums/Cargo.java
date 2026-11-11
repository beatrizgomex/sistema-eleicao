package br.com.eleicao.sistema.shared.enums;

public enum Cargo {
    DEPUTADO_ESTADUAL("Deputado Estadual"),
    DEPUTADO_FEDERAL("Deputado Federal"),
    SENADOR("Senador"),
    GOVERNADOR("Governador"),
    PRESIDENTE("Presidente da República");

    private final String descricao;

    Cargo(String descricao) { this.descricao = descricao; }

    public String getDescricao() { return descricao; }
}