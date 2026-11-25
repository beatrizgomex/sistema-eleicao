package br.com.eleicao.sistema.auth.controller;

import br.com.eleicao.sistema.auth.dto.SolicitacaoLogin;
import br.com.eleicao.sistema.auth.dto.RespostaLogin;
import br.com.eleicao.sistema.auth.dto.RespostaUsuarioLogado;
import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.auth.repository.UsuarioRepository;
import br.com.eleicao.sistema.auth.security.UsuarioAutenticado;
import br.com.eleicao.sistema.auth.service.ServicoAutenticacao;
import br.com.eleicao.sistema.auth.service.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Autenticação de usuários e emissão de token JWT")
public class ControladorAutenticacao {

    private final ServicoAutenticacao servicoAutenticacao;
    private final ObjectProvider<UsuarioLogado> provedorUsuarioLogado;
    private final UsuarioRepository repositorioUsuarios;

    @PostMapping("/login")
    @SecurityRequirements()
    @Operation(summary = "Autenticar usuário",
            description = "Valida login e senha e devolve um token JWT assinado com o perfil do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes ou em branco"),
            @ApiResponse(responseCode = "401", description = "Login ou senha inválidos"),
            @ApiResponse(responseCode = "429", description = "Tente novamente mais tarde")
    })
    public RespostaLogin autenticar(@Valid @RequestBody SolicitacaoLogin requisicao) {
        return servicoAutenticacao.autenticar(requisicao);
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar usuário autenticado",
            description = "Devolve o id, login e perfil do usuário autenticado pelo token JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do usuário autenticado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public RespostaUsuarioLogado consultarUsuarioLogado() {
        UsuarioLogado logado = provedorUsuarioLogado.getIfAvailable();
        if (logado == null || logado.getId() == null || logado.getPerfil() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        String login = (autenticacao != null && autenticacao.getPrincipal() instanceof UsuarioAutenticado autenticado)
                ? autenticado.login()
                : repositorioUsuarios.findById(logado.getId()).map(Usuario::getLogin).orElse(null);

        return new RespostaUsuarioLogado(logado.getId(), login, logado.getPerfil());
    }
}
