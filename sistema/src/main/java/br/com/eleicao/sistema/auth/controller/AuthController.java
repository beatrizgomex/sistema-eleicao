package br.com.eleicao.sistema.auth.controller;

import br.com.eleicao.sistema.auth.dto.LoginRequest;
import br.com.eleicao.sistema.auth.dto.LoginResponse;
import br.com.eleicao.sistema.auth.dto.UsuarioLogadoResponse;
import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.auth.repository.UsuarioRepository;
import br.com.eleicao.sistema.auth.security.UsuarioAutenticado;
import br.com.eleicao.sistema.auth.service.AuthService;
import br.com.eleicao.sistema.auth.service.UsuarioLogado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
public class AuthController {

    private final AuthService authService;
    private final ObjectProvider<UsuarioLogado> usuarioLogadoProvider;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário",
            description = "Valida login e senha e devolve um token JWT assinado com o perfil do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticação realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Campos obrigatórios ausentes ou em branco"),
            @ApiResponse(responseCode = "401", description = "Login ou senha inválidos")
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.autenticar(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar usuário autenticado",
            description = "Devolve o id, login e perfil do usuário autenticado pelo token JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados do usuário autenticado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public UsuarioLogadoResponse me() {
        UsuarioLogado logado = usuarioLogadoProvider.getIfAvailable();
        if (logado == null || logado.getId() == null || logado.getPerfil() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String login = (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado autenticado)
                ? autenticado.login()
                : usuarioRepository.findById(logado.getId()).map(Usuario::getLogin).orElse(null);

        return new UsuarioLogadoResponse(logado.getId(), login, logado.getPerfil());
    }
}
