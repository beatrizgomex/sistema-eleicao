package br.com.eleicao.sistema.candidato.dto;

import jakarta.validation.constraints.NotBlank;

public record PartidoRequest(@NotBlank String sigla, @NotBlank String nome) {}
