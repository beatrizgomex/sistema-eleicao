package br.com.eleicao.sistema.votacao.dto;

import br.com.eleicao.sistema.shared.enums.Cargo;
import br.com.eleicao.sistema.shared.enums.TipoVoto;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record VotoRequest(@NotNull Cargo cargo, @NotNull TipoVoto tipo, @Positive Integer numero) {}
