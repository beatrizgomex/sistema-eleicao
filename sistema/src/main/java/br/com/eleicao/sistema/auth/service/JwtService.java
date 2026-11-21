package br.com.eleicao.sistema.auth.service;

import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.auth.security.UsuarioAutenticado;
import br.com.eleicao.sistema.shared.enums.Perfil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

    private final SecretKey chave;
    private final Duration expiracao;
    private final Clock clock;

    @Autowired
    public JwtService(
            @Value("${jwt.secret}") String segredo,
            @Value("${jwt.expiracao-minutos}") long expiracaoMinutos) {
        this(segredo, expiracaoMinutos, Clock.systemUTC());
    }

    public JwtService(String segredo, long expiracaoMinutos, Clock clock) {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalArgumentException("jwt.secret deve ter pelo menos 32 bytes para assinatura HS256");
        }
        if (expiracaoMinutos <= 0) {
            throw new IllegalArgumentException("jwt.expiracao-minutos deve ser maior que zero");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracao = Duration.ofMinutes(expiracaoMinutos);
        this.clock = clock;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = clock.instant();
        Instant expiraEm = agora.plus(expiracao);
        return gerarToken(usuario, agora, expiraEm);
    }

    public Instant calcularExpiracao(Instant referencia) {
        return referencia.plus(expiracao);
    }

    public Instant agora() {
        return clock.instant();
    }

    public String gerarToken(Usuario usuario, Instant emitidoEm, Instant expiraEm) {
        return Jwts.builder()
                .subject(usuario.getLogin())
                .claim("id", usuario.getId())
                .claim("perfil", usuario.getPerfil().name())
                .issuedAt(Date.from(emitidoEm))
                .expiration(Date.from(expiraEm))
                .signWith(chave, Jwts.SIG.HS256)
                .compact();
    }

    public UsuarioAutenticado validar(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Token JWT ausente ou vazio");
        }
        Claims claims = Jwts.parser()
                .verifyWith(chave)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String login = claims.getSubject();
        Number idNumero = claims.get("id", Number.class);
        String perfilTexto = claims.get("perfil", String.class);

        if (login == null || login.isBlank() || idNumero == null || perfilTexto == null) {
            throw new JwtException("Token JWT sem as claims obrigatórias");
        }

        final Perfil perfil;
        try {
            perfil = Perfil.valueOf(perfilTexto);
        } catch (IllegalArgumentException e) {
            throw new JwtException("Perfil inválido no token JWT", e);
        }

        return new UsuarioAutenticado(idNumero.longValue(), login, perfil);
    }
}
