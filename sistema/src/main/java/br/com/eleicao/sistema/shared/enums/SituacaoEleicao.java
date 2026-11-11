package br.com.eleicao.sistema.shared.enums;

public enum SituacaoEleicao {
    AGENDADA, ABERTA, ENCERRADA;

    public boolean podeIrPara(SituacaoEleicao destino) {
        return (this == AGENDADA && destino == ABERTA)
                || (this == ABERTA && destino == ENCERRADA);
    }
}