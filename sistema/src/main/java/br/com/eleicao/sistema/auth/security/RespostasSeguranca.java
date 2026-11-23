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

    public AuthenticationEntryPoint aoExigirAutenticacao() {
        return (requisicao, resposta, erroAutenticacao) -> {
            Object motivoJwt = requisicao.getAttribute(FiltroAutenticacaoJwt.ATRIBUTO_ERRO_JWT);
            String mensagem = motivoJwt instanceof String texto
                    ? texto
                    : "Autenticação necessária para acessar este recurso";
            escreverErroJson(resposta, requisicao, HttpStatus.UNAUTHORIZED, mensagem);
        };
    }

    public AccessDeniedHandler aoNegarAcesso() {
        return (requisicao, resposta, erroAcessoNegado) ->
                escreverErroJson(resposta, requisicao, HttpStatus.FORBIDDEN,
                        "Acesso negado para o perfil do usuário autenticado");
    }

    private void escreverErroJson(
            HttpServletResponse resposta,
            HttpServletRequest requisicao,
            HttpStatus status,
            String mensagem) throws IOException {

        resposta.setStatus(status.value());
        resposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String corpo = """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s"}"""
                .formatted(
                        Instant.now().toString(),
                        status.value(),
                        escaparJson(status.getReasonPhrase()),
                        escaparJson(mensagem),
                        escaparJson(requisicao.getRequestURI())
                );

        resposta.getWriter().write(corpo);
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
