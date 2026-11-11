package br.com.eleicao.sistema.auth.service;

import br.com.eleicao.sistema.shared.enums.Perfil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
        return h == null ? null : Long.valueOf(h);
    }

    @Override
    public Perfil getPerfil() {
        String h = request.getHeader("X-Perfil");
        return h == null ? null : Perfil.valueOf(h.toUpperCase());
    }
}