package br.com.eleicao.sistema.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class RespostasSeguranca {

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            Object motivoJwt = request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERRO_JWT);
            String mensagem = motivoJwt instanceof String texto
                    ? texto
                    : "Autenticação necessária para acessar este recurso";
            escreverErroJson(response, request, HttpStatus.UNAUTHORIZED, mensagem);
        };
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) ->
                escreverErroJson(response, request, HttpStatus.FORBIDDEN,
                        "Acesso negado para o perfil do usuário autenticado");
    }

    private void escreverErroJson(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String mensagem) throws IOException {

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String corpo = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s"}"""
                .formatted(
                        Instant.now().toString(),
                        status.value(),
                        escaparJson(status.getReasonPhrase()),
                        escaparJson(mensagem),
                        escaparJson(request.getRequestURI())
                );

        response.getWriter().write(corpo);
    }

    private String escaparJson(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
