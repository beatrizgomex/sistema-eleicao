package br.com.eleicao.sistema.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record SolicitacaoLogin(
        @Schema(description = "Login do usuário", example = "chefe")
        @NotBlank(message = "Login é obrigatório")
        String login,

        @Schema(description = "Senha do usuário", example = "chefe123")
        @NotBlank(message = "Senha é obrigatória")
        String senha
) {
}
