package br.com.eleicao.sistema.auth.controller;

import br.com.eleicao.sistema.auth.dto.LoginRequest;
import br.com.eleicao.sistema.auth.dto.LoginResponse;
import br.com.eleicao.sistema.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Autenticação de usuários e emissão de token JWT")
public class AuthController {

    private final AuthService authService;

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
}
