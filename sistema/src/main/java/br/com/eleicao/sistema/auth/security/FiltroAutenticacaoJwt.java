package br.com.eleicao.sistema.auth.security;

import br.com.eleicao.sistema.auth.service.ServicoJwt;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class FiltroAutenticacaoJwt extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";
    public static final String ATRIBUTO_ERRO_JWT = "jwt.erro";

    private final ServicoJwt servicoJwt;

    public FiltroAutenticacaoJwt(ServicoJwt servicoJwt) {
        this.servicoJwt = servicoJwt;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest requisicao,
            HttpServletResponse resposta,
            FilterChain cadeiaFiltros) throws ServletException, IOException {

        String cabecalho = requisicao.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecalho != null && cabecalho.startsWith(PREFIXO_BEARER)) {
            String token = cabecalho.substring(PREFIXO_BEARER.length()).trim();
            try {
                UsuarioAutenticado usuario = servicoJwt.validar(token);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SimpleGrantedAuthority autoridade =
                            new SimpleGrantedAuthority("ROLE_" + usuario.perfil().name());
                    UsernamePasswordAuthenticationToken autenticacao =
                            new UsernamePasswordAuthenticationToken(usuario, null, List.of(autoridade));
                    autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(requisicao));
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                }
            } catch (JwtException | IllegalArgumentException erro) {
                SecurityContextHolder.clearContext();
                requisicao.setAttribute(ATRIBUTO_ERRO_JWT, "Token JWT inválido ou expirado");
            }
        }

        cadeiaFiltros.doFilter(requisicao, resposta);
    }
}
