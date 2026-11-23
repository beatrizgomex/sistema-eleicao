package br.com.eleicao.sistema.votacao.dto;

import br.com.eleicao.sistema.candidato.dto.CandidatoResponse;
import br.com.eleicao.sistema.shared.enums.Cargo;

import java.util.List;

public record CargoNaCedulaResponse(Cargo cargo, String descricao, List<CandidatoResponse> candidatos) {}
