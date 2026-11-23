package br.com.eleicao.sistema.votacao.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CedulaRequest(@NotNull Long eleicaoId,
                            @NotEmpty List<@Valid @NotNull VotoRequest> votos) {}
