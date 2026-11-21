package br.com.eleicao.sistema.auth.dto;

import br.com.eleicao.sistema.shared.enums.Perfil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record LoginResponse(
        @Schema(description = "Token JWT assinado", example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,

        @Schema(description = "Tipo do token de autenticação", example = "Bearer")
        String tipo,

        @Schema(description = "Instante de expiração do token em UTC", example = "2026-11-20T22:30:00Z")
        Instant expiraEm,

        @Schema(description = "Perfil do usuário autenticado", example = "CHEFE")
        Perfil perfil
) {
}
