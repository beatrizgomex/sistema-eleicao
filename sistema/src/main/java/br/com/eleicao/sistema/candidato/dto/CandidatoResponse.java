package br.com.eleicao.sistema.candidato.dto;


import br.com.eleicao.sistema.shared.enums.Cargo;

public record CandidatoResponse(Long id, String nome, Integer numero, Cargo cargo,
                                Long partidoId, String partidoSigla, Long eleicaoId) {}