package br.com.eleicao.sistema.auth.service;

import br.com.eleicao.sistema.shared.enums.Perfil;

public interface UsuarioLogado {
    Long getId();
    Perfil getPerfil();
}