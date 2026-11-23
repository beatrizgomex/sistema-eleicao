package br.com.eleicao.sistema.auth.security;

import br.com.eleicao.sistema.shared.enums.Perfil;

public record UsuarioAutenticado(Long id, String login, Perfil perfil) {
}
