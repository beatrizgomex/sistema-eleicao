package br.com.eleicao.sistema.auth.security;

import br.com.eleicao.sistema.auth.service.JwtService;
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

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";
    public static final String ATRIBUTO_ERRO_JWT = "jwt.erro";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecalho != null && cabecalho.startsWith(PREFIXO_BEARER)) {
            String token = cabecalho.substring(PREFIXO_BEARER.length()).trim();
            try {
                UsuarioAutenticado usuario = jwtService.validar(token);
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    SimpleGrantedAuthority autoridade =
                            new SimpleGrantedAuthority("ROLE_" + usuario.perfil().name());
                    UsernamePasswordAuthenticationToken autenticacao =
                            new UsernamePasswordAuthenticationToken(usuario, null, List.of(autoridade));
                    autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                }
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                request.setAttribute(ATRIBUTO_ERRO_JWT, "Token JWT inválido ou expirado");
            }
        }

        filterChain.doFilter(request, response);
    }
}
