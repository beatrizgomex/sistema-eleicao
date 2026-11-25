package br.com.eleicao.sistema.service;

import br.com.eleicao.sistema.auth.dto.SolicitacaoLogin;
import br.com.eleicao.sistema.auth.dto.RespostaLogin;
import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.auth.service.ServicoAutenticacao;
import br.com.eleicao.sistema.auth.service.ServicoJwt;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Serviço de autenticação")
class TesteServicoAutenticacao {

    @Mock
    private UsuarioService servicoUsuario;

    @Mock
    private PasswordEncoder codificadorSenha;

    @Mock
    private ServicoJwt servicoJwt;

    @InjectMocks
    private ServicoAutenticacao servicoAutenticacao;

    private Usuario usuarioValido() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setLogin("maria");
        usuario.setSenha("$2a$10$hashBcrypt");
        usuario.setPerfil(Perfil.ELEITOR);
        return usuario;
    }

    @Test
    @DisplayName("retorna token JWT quando credenciais estão corretas")
    void deveAutenticarComCredenciaisValidas() {
        Usuario usuario = usuarioValido();
        Instant agora = Instant.parse("2026-11-20T12:00:00Z");
        Instant expiraEm = Instant.parse("2026-11-20T13:00:00Z");

        when(servicoUsuario.buscarPorLogin("maria")).thenReturn(Optional.of(usuario));
        when(codificadorSenha.matches("senha123", "$2a$10$hashBcrypt")).thenReturn(true);
        when(servicoJwt.agora()).thenReturn(agora);
        when(servicoJwt.calcularExpiracao(agora)).thenReturn(expiraEm);
        when(servicoJwt.gerarToken(usuario, agora, expiraEm)).thenReturn("token-jwt-assinado");

        RespostaLogin resposta = servicoAutenticacao.autenticar(new SolicitacaoLogin("maria", "senha123"));

        assertEquals("token-jwt-assinado", resposta.token());
        assertEquals("Bearer", resposta.tipo());
        assertEquals(expiraEm, resposta.expiraEm());
        assertEquals(Perfil.ELEITOR, resposta.perfil());
    }

    @Test
    @DisplayName("retorna 401 quando senha está incorreta")
    void deveRejeitarSenhaIncorreta() {
        Usuario usuario = usuarioValido();
        when(servicoUsuario.buscarPorLogin("maria")).thenReturn(Optional.of(usuario));
        when(codificadorSenha.matches("errada", "$2a$10$hashBcrypt")).thenReturn(false);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servicoAutenticacao.autenticar(new SolicitacaoLogin("maria", "errada")));

        assertEquals(401, erro.getStatusCode().value());
        assertEquals("Login ou senha inválidos", erro.getReason());
        verify(servicoUsuario).registrarFalhaLogin(usuario, 1);
        verify(servicoJwt, never()).gerarToken(any());
    }

    @Test
    @DisplayName("retorna 401 quando login não existe")
    void deveRejeitarLoginInexistente() {
        when(servicoUsuario.buscarPorLogin("desconhecido")).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servicoAutenticacao.autenticar(new SolicitacaoLogin("desconhecido", "qualquer")));

        assertEquals(401, erro.getStatusCode().value());
        assertEquals("Login ou senha inválidos", erro.getReason());
        verifyNoInteractions(codificadorSenha, servicoJwt);
    }

    @Test
    @DisplayName("bloqueia acesso por 30 minutos na quinta falha consecutiva sem expor o tempo na mensagem")
    void deveBloquearPor30MinutosNaQuintaFalhaSemExporTempo() {
        Usuario usuario = usuarioValido();
        usuario.setTentativasFalhas(4);
        Instant agora = Instant.parse("2026-11-20T12:00:00Z");

        when(servicoUsuario.buscarPorLogin("maria")).thenReturn(Optional.of(usuario));
        when(servicoJwt.agora()).thenReturn(agora);
        when(codificadorSenha.matches("errada", "$2a$10$hashBcrypt")).thenReturn(false);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servicoAutenticacao.autenticar(new SolicitacaoLogin("maria", "errada")));

        assertEquals(429, erro.getStatusCode().value());
        assertEquals("Tente novamente mais tarde", erro.getReason());
        assertFalse(erro.getReason().contains("30"));
        assertFalse(erro.getReason().toLowerCase().contains("minuto"));
        verify(servicoUsuario).registrarBloqueioLogin(
                eq(usuario), eq(5), eq(agora.plus(Duration.ofMinutes(30))));
        verify(servicoJwt, never()).gerarToken(any());
    }

    @Test
    @DisplayName("mantém bloqueio durante os 30 minutos mesmo com a senha correta")
    void deveManterBloqueioDurante30MinutosMesmoComSenhaCorreta() {
        Usuario usuario = usuarioValido();
        Instant inicioBloqueio = Instant.parse("2026-11-20T12:00:00Z");
        usuario.setTentativasFalhas(5);
        usuario.setBloqueadoAte(inicioBloqueio.plus(Duration.ofMinutes(30)));

        when(servicoUsuario.buscarPorLogin("maria")).thenReturn(Optional.of(usuario));
        when(servicoJwt.agora()).thenReturn(inicioBloqueio.plus(Duration.ofMinutes(29)).plusSeconds(59));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servicoAutenticacao.autenticar(new SolicitacaoLogin("maria", "senha123")));

        assertEquals(429, erro.getStatusCode().value());
        assertEquals("Tente novamente mais tarde", erro.getReason());
        verifyNoInteractions(codificadorSenha);
        verify(servicoJwt, never()).gerarToken(any());
    }

    @Test
    @DisplayName("libera o login após expirar os 30 minutos de bloqueio e limpa tentativas")
    void deveLiberarAcessoApos30MinutosDeBloqueio() {
        Usuario usuario = usuarioValido();
        Instant inicioBloqueio = Instant.parse("2026-11-20T12:00:00Z");
        Instant fimBloqueio = inicioBloqueio.plus(Duration.ofMinutes(30));
        Instant expiraEm = fimBloqueio.plus(Duration.ofMinutes(60));
        usuario.setTentativasFalhas(5);
        usuario.setBloqueadoAte(fimBloqueio);

        when(servicoUsuario.buscarPorLogin("maria")).thenReturn(Optional.of(usuario));
        when(servicoJwt.agora()).thenReturn(fimBloqueio);
        when(codificadorSenha.matches("senha123", "$2a$10$hashBcrypt")).thenReturn(true);
        when(servicoJwt.calcularExpiracao(fimBloqueio)).thenReturn(expiraEm);
        when(servicoJwt.gerarToken(usuario, fimBloqueio, expiraEm)).thenReturn("novo-token");

        RespostaLogin resposta = servicoAutenticacao.autenticar(new SolicitacaoLogin("maria", "senha123"));

        assertEquals("novo-token", resposta.token());
        verify(servicoUsuario).limparTentativasLogin(usuario);
    }

    @Test
    @DisplayName("reseta tentativas falhas ao autenticar com sucesso antes da quinta falha")
    void deveResetarTentativasAoAcertarSenhaAntesDoBloqueio() {
        Usuario usuario = usuarioValido();
        usuario.setTentativasFalhas(3);
        Instant agora = Instant.parse("2026-11-20T12:00:00Z");
        Instant expiraEm = Instant.parse("2026-11-20T13:00:00Z");

        when(servicoUsuario.buscarPorLogin("maria")).thenReturn(Optional.of(usuario));
        when(servicoJwt.agora()).thenReturn(agora);
        when(codificadorSenha.matches("senha123", "$2a$10$hashBcrypt")).thenReturn(true);
        when(servicoJwt.calcularExpiracao(agora)).thenReturn(expiraEm);
        when(servicoJwt.gerarToken(usuario, agora, expiraEm)).thenReturn("token-ok");

        RespostaLogin resposta = servicoAutenticacao.autenticar(new SolicitacaoLogin("maria", "senha123"));

        assertEquals("token-ok", resposta.token());
        verify(servicoUsuario).limparTentativasLogin(usuario);
    }

    @Test
    @DisplayName("reinicia contagem em 1 falha quando erra a senha após expirar o bloqueio de 30 minutos")
    void deveReiniciarContagemEmUmaFalhaAposExpirarBloqueio() {
        Usuario usuario = usuarioValido();
        Instant inicioBloqueio = Instant.parse("2026-11-20T12:00:00Z");
        Instant aposBloqueio = inicioBloqueio.plus(Duration.ofMinutes(31));
        usuario.setTentativasFalhas(5);
        usuario.setBloqueadoAte(inicioBloqueio.plus(Duration.ofMinutes(30)));

        when(servicoUsuario.buscarPorLogin("maria")).thenReturn(Optional.of(usuario));
        when(servicoJwt.agora()).thenReturn(aposBloqueio);
        when(codificadorSenha.matches("errada", "$2a$10$hashBcrypt")).thenReturn(false);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> servicoAutenticacao.autenticar(new SolicitacaoLogin("maria", "errada")));

        assertEquals(401, erro.getStatusCode().value());
        assertEquals("Login ou senha inválidos", erro.getReason());
        verify(servicoUsuario).registrarFalhaLogin(usuario, 1);
    }
}
