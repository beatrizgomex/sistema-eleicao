package br.com.eleicao.sistema.auth.service;

import br.com.eleicao.sistema.auth.security.UsuarioAutenticado;
import br.com.eleicao.sistema.shared.enums.Perfil;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Profile("!dev")
public class UsuarioLogadoJwt implements UsuarioLogado {

    @Override
    public Long getId() {
        UsuarioAutenticado usuario = obterUsuarioAutenticado();
        return usuario == null ? null : usuario.id();
    }

    @Override
    public Perfil getPerfil() {
        UsuarioAutenticado usuario = obterUsuarioAutenticado();
        return usuario == null ? null : usuario.perfil();
    }

    private UsuarioAutenticado obterUsuarioAutenticado() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            return null;
        }
        if (autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return usuario;
        }
        return null;
    }
}
