package br.com.eleicao.sistema.auth.dto;

import br.com.eleicao.sistema.shared.enums.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;

public record UsuarioLogadoResponse(
        @Schema(description = "Identificador do usuário autenticado", example = "1")
        Long id,

        @Schema(description = "Login do usuário autenticado", example = "chefe")
        String login,

        @Schema(description = "Perfil de acesso do usuário autenticado", example = "CHEFE")
        Perfil perfil
) {
}
