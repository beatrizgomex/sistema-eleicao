package br.com.eleicao.sistema.candidato.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CandidatoUpdateRequest(@NotBlank String nome, @NotNull Long partidoId) {}

