package br.com.eleicao.sistema.auth;

import br.com.eleicao.sistema.auth.repository.UsuarioRepository;
import br.com.eleicao.sistema.auth.service.UsuarioService;
import br.com.eleicao.sistema.shared.enums.Perfil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracaoChefeInicial {

    @Bean
    CommandLineRunner criarChefeInicial(
            UsuarioRepository repositorioUsuarios,
            UsuarioService servicoUsuario,
            @Value("${chefe.login:chefe}") String login,
            @Value("${chefe.senha:}") String senha) {
        return argumentos -> {
            if (repositorioUsuarios.existsByPerfil(Perfil.CHEFE)) {
                return;
            }
            if (senha.isBlank()) {
                throw new IllegalStateException("Configure CHEFE_SENHA para criar o primeiro usuário CHEFE");
            }
            servicoUsuario.criar(login, senha, Perfil.CHEFE);
        };
    }
}
