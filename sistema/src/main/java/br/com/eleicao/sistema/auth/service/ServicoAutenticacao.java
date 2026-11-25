package br.com.eleicao.sistema.auth.service;

import br.com.eleicao.sistema.auth.dto.SolicitacaoLogin;
import br.com.eleicao.sistema.auth.dto.RespostaLogin;
import br.com.eleicao.sistema.auth.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ServicoAutenticacao {

    public static final int MAXIMO_TENTATIVAS_FALHAS = 5;
    public static final Duration DURACAO_BLOQUEIO = Duration.ofMinutes(30);

    private static final String CREDENCIAIS_INVALIDAS = "Login ou senha inválidos";
    private static final String MENSAGEM_BLOQUEIO = "Tente novamente mais tarde";

    private final UsuarioService servicoUsuario;
    private final PasswordEncoder codificadorSenha;
    private final ServicoJwt servicoJwt;

    public RespostaLogin autenticar(SolicitacaoLogin requisicao) {
        Usuario usuario = servicoUsuario.buscarPorLogin(requisicao.login())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS));

        Instant agora = instanteAtual();

        boolean bloqueioExpirado = false;
        if (usuario.getBloqueadoAte() != null) {
            if (agora.isBefore(usuario.getBloqueadoAte())) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, MENSAGEM_BLOQUEIO);
            }
            bloqueioExpirado = true;
        }

        if (!codificadorSenha.matches(requisicao.senha(), usuario.getSenha())) {
            int novasTentativas = bloqueioExpirado ? 1 : usuario.getTentativasFalhas() + 1;
            if (novasTentativas >= MAXIMO_TENTATIVAS_FALHAS) {
                servicoUsuario.registrarBloqueioLogin(usuario, novasTentativas, agora.plus(DURACAO_BLOQUEIO));
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, MENSAGEM_BLOQUEIO);
            }
            servicoUsuario.registrarFalhaLogin(usuario, novasTentativas);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIAIS_INVALIDAS);
        }

        if (bloqueioExpirado || usuario.getTentativasFalhas() > 0) {
            servicoUsuario.limparTentativasLogin(usuario);
        }

        Instant expiraEm = servicoJwt.calcularExpiracao(agora);
        String token = servicoJwt.gerarToken(usuario, agora, expiraEm);

        return new RespostaLogin(token, "Bearer", expiraEm, usuario.getPerfil());
    }

    private Instant instanteAtual() {
        Instant agora = servicoJwt.agora();
        return agora != null ? agora : Instant.now();
    }
}
