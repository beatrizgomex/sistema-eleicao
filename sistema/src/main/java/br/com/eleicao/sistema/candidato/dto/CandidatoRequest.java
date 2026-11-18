package br.com.eleicao.sistema.candidato.dto;

import br.com.eleicao.sistema.shared.enums.Cargo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CandidatoRequest(@NotBlank String nome, @NotNull @Positive Integer numero,
                               @NotNull Cargo cargo, @NotNull Long partidoId,
                               @NotNull Long eleicaoId,
                               @NotBlank String login, @NotBlank String senha) {}
