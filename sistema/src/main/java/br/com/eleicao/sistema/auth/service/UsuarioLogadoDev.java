package br.com.eleicao.sistema.auth.service;

import br.com.eleicao.sistema.shared.enums.Perfil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Profile("dev")
public class UsuarioLogadoDev implements UsuarioLogado {

    private final HttpServletRequest request;

    public UsuarioLogadoDev(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public Long getId() {
        String h = request.getHeader("X-Usuario-Id");
        if (h == null || h.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(h.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cabeçalho X-Usuario-Id inválido");
        }
    }

    @Override
    public Perfil getPerfil() {
        String h = request.getHeader("X-Perfil");
        if (h == null || h.isBlank()) {
            return null;
        }
        try {
            return Perfil.valueOf(h.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cabeçalho X-Perfil inválido");
        }
    }
}