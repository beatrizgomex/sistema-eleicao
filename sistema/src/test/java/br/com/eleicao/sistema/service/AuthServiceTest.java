package br.com.eleicao.sistema.service;

import br.com.eleicao.sistema.auth.dto.LoginRequest;
import br.com.eleicao.sistema.auth.dto.LoginResponse;
import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.auth.service.AuthService;
import br.com.eleicao.sistema.auth.service.JwtService;
import br.com.eleicao.sistema.auth.service.UsuarioService;
import br.com.eleicao.sistema.shared.enums.Perfil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioValido() {
        Usuario u = new Usuario();
        u.setId(10L);
        u.setLogin("maria");
        u.setSenha("$2a$10$hashBcrypt");
        u.setPerfil(Perfil.ELEITOR);
        return u;
    }

    @Test
    @DisplayName("retorna token JWT quando credenciais estão corretas")
    void deveAutenticarComCredenciaisValidas() {
        Usuario usuario = usuarioValido();
        Instant agora = Instant.parse("2026-11-20T12:00:00Z");
        Instant expiraEm = Instant.parse("2026-11-20T13:00:00Z");

        when(usuarioService.buscarPorLogin("maria")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "$2a$10$hashBcrypt")).thenReturn(true);
        when(jwtService.agora()).thenReturn(agora);
        when(jwtService.calcularExpiracao(agora)).thenReturn(expiraEm);
        when(jwtService.gerarToken(usuario, agora, expiraEm)).thenReturn("token-jwt-assinado");

        LoginResponse resposta = authService.autenticar(new LoginRequest("maria", "senha123"));

        assertEquals("token-jwt-assinado", resposta.token());
        assertEquals("Bearer", resposta.tipo());
        assertEquals(expiraEm, resposta.expiraEm());
        assertEquals(Perfil.ELEITOR, resposta.perfil());
    }

    @Test
    @DisplayName("retorna 401 quando senha está incorreta")
    void deveRejeitarSenhaIncorreta() {
        Usuario usuario = usuarioValido();
        when(usuarioService.buscarPorLogin("maria")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada", "$2a$10$hashBcrypt")).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.autenticar(new LoginRequest("maria", "errada")));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Login ou senha inválidos", ex.getReason());
        verify(jwtService, never()).gerarToken(any());
    }

    @Test
    @DisplayName("retorna 401 quando login não existe")
    void deveRejeitarLoginInexistente() {
        when(usuarioService.buscarPorLogin("desconhecido")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.autenticar(new LoginRequest("desconhecido", "qualquer")));

        assertEquals(401, ex.getStatusCode().value());
        assertEquals("Login ou senha inválidos", ex.getReason());
        verifyNoInteractions(passwordEncoder, jwtService);
    }
}
