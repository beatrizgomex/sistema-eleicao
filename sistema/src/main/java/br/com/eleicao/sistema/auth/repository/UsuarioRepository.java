package br.com.eleicao.sistema.auth.repository;

import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.shared.enums.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByPerfil(Perfil perfil);
}