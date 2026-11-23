package br.com.eleicao.sistema.apuracao.dto;

import br.com.eleicao.sistema.shared.enums.Cargo;

import java.util.List;

public record ResultadoCargoResponse(Cargo cargo,
                                     List<CandidatoVotosResponse> candidatos,
                                     long brancos,
                                     long nulos,
                                     CandidatoVotosResponse vencedor,
                                     boolean empate) {
}
