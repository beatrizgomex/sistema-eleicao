package br.com.eleicao.sistema.apuracao.dto;

import br.com.eleicao.sistema.shared.enums.Cargo;

import java.util.List;

public record ApuracaoParcialResponse(Cargo cargo,
                                      List<CandidatoVotosResponse> candidatos,
                                      long brancos,
                                      long nulos) {
}
