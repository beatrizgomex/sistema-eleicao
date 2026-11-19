package br.com.eleicao.sistema.eleitor.dto;

import jakarta.validation.constraints.NotBlank;

public record EleitorRequest(
        @NotBlank String nome,
        @NotBlank String documento,
        @NotBlank String login,
        @NotBlank String senha) {}