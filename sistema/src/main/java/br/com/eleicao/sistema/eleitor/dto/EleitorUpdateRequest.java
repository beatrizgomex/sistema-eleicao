package br.com.eleicao.sistema.eleitor.dto;

import jakarta.validation.constraints.NotBlank;

public record EleitorUpdateRequest(
        @NotBlank String nome,
        @NotBlank String documento) {}