package br.com.eleicao.sistema.auth.service;

import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.shared.enums.Perfil;
import java.util.Optional;

public interface UsuarioService {
    Usuario criar(String login, String senha, Perfil perfil);
    Optional<Usuario> buscarPorLogin(String login);
}