package br.com.eleicao.sistema.service;

import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.auth.security.UsuarioAutenticado;
import br.com.eleicao.sistema.auth.service.JwtService;
import br.com.eleicao.sistema.shared.enums.Perfil;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SEGREDO_VALIDO = "chave-secreta-de-teste-com-pelo-menos-32-bytes!";
    private static final String OUTRO_SEGREDO = "outra-chave-secreta-diferente-com-32-bytes-ok!";
    private static final Instant INSTANTE_BASE = Instant.parse("2026-11-20T12:00:00Z");

    private Usuario criarUsuario(Long id, String login, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setLogin(login);
        usuario.setSenha("$2a$10$hash");
        usuario.setPerfil(perfil);
        return usuario;
    }

    @Test
    @DisplayName("gera e valida token preservando id, login e perfil")
    void deveGerarEValidarTokenValido() {
        Clock clock = Clock.fixed(INSTANTE_BASE, ZoneOffset.UTC);
        JwtService jwtService = new JwtService(SEGREDO_VALIDO, 60, clock);
        Usuario usuario = criarUsuario(42L, "maria", Perfil.ELEITOR);

        String token = jwtService.gerarToken(usuario);
        assertNotNull(token);

        UsuarioAutenticado autenticado = jwtService.validar(token);
        assertEquals(42L, autenticado.id());
        assertEquals("maria", autenticado.login());
        assertEquals(Perfil.ELEITOR, autenticado.perfil());
    }

    @Test
    @DisplayName("rejeita segredo com menos de 32 bytes na inicialização")
    void deveRejeitarSegredoCurto() {
        Clock clock = Clock.fixed(INSTANTE_BASE, ZoneOffset.UTC);
        assertThrows(IllegalArgumentException.class,
                () -> new JwtService("segredo-curto", 60, clock));
    }

    @Test
    @DisplayName("rejeita token expirado")
    void deveRejeitarTokenExpirado() {
        Clock emissao = Clock.fixed(INSTANTE_BASE, ZoneOffset.UTC);
        JwtService emissor = new JwtService(SEGREDO_VALIDO, 60, emissao);
        String token = emissor.gerarToken(criarUsuario(1L, "chefe", Perfil.CHEFE));

        Clock aposExpiracao = Clock.offset(emissao, Duration.ofMinutes(61));
        JwtService validador = new JwtService(SEGREDO_VALIDO, 60, aposExpiracao);

        assertThrows(JwtException.class, () -> validador.validar(token));
    }

    @Test
    @DisplayName("rejeita token adulterado")
    void deveRejeitarTokenAdulterado() {
        Clock clock = Clock.fixed(INSTANTE_BASE, ZoneOffset.UTC);
        JwtService jwtService = new JwtService(SEGREDO_VALIDO, 60, clock);
        String token = jwtService.gerarToken(criarUsuario(7L, "joao", Perfil.CANDIDATO));

        String[] partes = token.split("\\.");
        char ultimo = partes[2].charAt(partes[2].length() - 1);
        char trocado = ultimo == 'a' ? 'b' : 'a';
        String adulterado = partes[0] + "." + partes[1] + "."
                + partes[2].substring(0, partes[2].length() - 1) + trocado;

        assertThrows(JwtException.class, () -> jwtService.validar(adulterado));
    }

    @Test
    @DisplayName("rejeita token assinado com outra chave")
    void deveRejeitarTokenDeOutraChave() {
        Clock clock = Clock.fixed(INSTANTE_BASE, ZoneOffset.UTC);
        JwtService outroEmissor = new JwtService(OUTRO_SEGREDO, 60, clock);
        JwtService validador = new JwtService(SEGREDO_VALIDO, 60, clock);

        String token = outroEmissor.gerarToken(criarUsuario(1L, "chefe", Perfil.CHEFE));

        assertThrows(JwtException.class, () -> validador.validar(token));
    }
}
