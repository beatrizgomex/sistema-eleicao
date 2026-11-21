package br.com.eleicao.sistema.service;

import br.com.eleicao.sistema.auth.security.UsuarioAutenticado;
import br.com.eleicao.sistema.auth.service.UsuarioLogadoJwt;
import br.com.eleicao.sistema.shared.enums.Perfil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("UsuarioLogadoJwt")
class UsuarioLogadoJwtTest {

    private final UsuarioLogadoJwt usuarioLogadoJwt = new UsuarioLogadoJwt();

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("retorna id e perfil quando SecurityContext contém UsuarioAutenticado")
    void deveRetornarIdEPerfilDoContexto() {
        UsuarioAutenticado principal = new UsuarioAutenticado(15L, "eleitor1", Perfil.ELEITOR);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ELEITOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertEquals(15L, usuarioLogadoJwt.getId());
        assertEquals(Perfil.ELEITOR, usuarioLogadoJwt.getPerfil());
    }

    @Test
    @DisplayName("retorna null quando SecurityContext está vazio")
    void deveRetornarNullSemAutenticacao() {
        SecurityContextHolder.clearContext();

        assertNull(usuarioLogadoJwt.getId());
        assertNull(usuarioLogadoJwt.getPerfil());
    }

    @Test
    @DisplayName("retorna null quando principal não é UsuarioAutenticado")
    void deveRetornarNullParaPrincipalDesconhecido() {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertNull(usuarioLogadoJwt.getId());
        assertNull(usuarioLogadoJwt.getPerfil());
    }
}
