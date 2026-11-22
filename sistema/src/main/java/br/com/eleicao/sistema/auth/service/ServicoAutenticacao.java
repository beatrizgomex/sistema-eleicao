package br.com.eleicao.sistema.auth.service;

import br.com.eleicao.sistema.auth.dto.SolicitacaoLogin;
import br.com.eleicao.sistema.auth.dto.RespostaLogin;
import br.com.eleicao.sistema.auth.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ServicoAutenticacao {

    private static final String CREDENCIAIS_INVALIDAS = "Login ou senha inválidos";

    private final UsuarioService servicoUsuario;
    private final PasswordEncoder codificadorSenha;
    private final ServicoJwt servicoJwt;

    public RespostaLogin autenticar(SolicitacaoLogin requisicao) {
        Usuario usuario = servicoUsuario.buscarPorLogin(requisicao.login())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS));

        if (!codificadorSenha.matches(requisicao.senha(), usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS);
        }

        Instant agora = servicoJwt.agora();
        Instant expiraEm = servicoJwt.calcularExpiracao(agora);
        String token = servicoJwt.gerarToken(usuario, agora, expiraEm);

        return new RespostaLogin(token, "Bearer", expiraEm, usuario.getPerfil());
    }
}
