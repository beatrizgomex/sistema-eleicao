package br.com.eleicao.sistema.auth.service;

import br.com.eleicao.sistema.auth.dto.LoginRequest;
import br.com.eleicao.sistema.auth.dto.LoginResponse;
import br.com.eleicao.sistema.auth.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String CREDENCIAIS_INVALIDAS = "Login ou senha inválidos";

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse autenticar(LoginRequest request) {
        Usuario usuario = usuarioService.buscarPorLogin(request.login())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS);
        }

        Instant agora = jwtService.agora();
        Instant expiraEm = jwtService.calcularExpiracao(agora);
        String token = jwtService.gerarToken(usuario, agora, expiraEm);

        return new LoginResponse(token, "Bearer", expiraEm, usuario.getPerfil());
    }
}
