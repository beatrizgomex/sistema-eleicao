package br.com.eleicao.sistema.auth.service;

import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.auth.repository.UsuarioRepository;
import br.com.eleicao.sistema.shared.enums.Perfil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Usuario criar(String login, String senha, Perfil perfil) {
        if (usuarioRepository.existsByLogin(login)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Login já em uso");
        }
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setPerfil(perfil);
        return usuarioRepository.save(usuario);
    }

    public Optional<Usuario> buscarPorLogin(String login) {
        return usuarioRepository.findByLogin(login);
    }

    @Transactional
    public void registrarFalhaLogin(Usuario usuario, int tentativasFalhas) {
        usuario.setTentativasFalhas(tentativasFalhas);
        usuario.setBloqueadoAte(null);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void registrarBloqueioLogin(Usuario usuario, int tentativasFalhas, Instant bloqueadoAte) {
        usuario.setTentativasFalhas(tentativasFalhas);
        usuario.setBloqueadoAte(bloqueadoAte);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void limparTentativasLogin(Usuario usuario) {
        usuario.setTentativasFalhas(0);
        usuario.setBloqueadoAte(null);
        usuarioRepository.save(usuario);
    }
}