package br.com.eleicao.sistema.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Sistema de Simulação de Eleição Eletrônica - API",
                version = "1.0.0",
                description = """
                        API REST para gerenciamento de eleições, cadastro de participantes (eleitores, candidatos e partidos), \
                        votação secreta e apuração de resultados.

                        ### Como autenticar no Swagger UI
                        1. Execute `POST /autenticacao/login` informando `login` e `senha` (por padrão, o Chefe inicial é `chefe` / `chefe123`).
                        2. Copie o valor do campo `token` retornado na resposta.
                        3. Clique no botão **Authorize** no topo da página, cole apenas o token JWT e confirme."""
        ),
        security = @SecurityRequirement(name = ConfiguracaoOpenApi.ESQUEMA_BEARER)
)
@SecurityScheme(
        name = ConfiguracaoOpenApi.ESQUEMA_BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Informe o token JWT obtido em POST /auth/login"
)
public class ConfiguracaoOpenApi {

    public static final String ESQUEMA_BEARER = "bearerAuth";

    @Bean
    public OpenApiCustomizer documentarRespostasSeguranca() {
        return especificacao -> {
            if (especificacao.getPaths() == null) {
                return;
            }
            especificacao.getPaths().forEach((caminho, item) -> item.readOperations().forEach(operacao -> {
                if ("/auth/login".equals(caminho)) {
                    operacao.setSecurity(List.of());
                    return;
                }

                ApiResponses respostas = operacao.getResponses();
                if (respostas == null) {
                    respostas = new ApiResponses();
                    operacao.setResponses(respostas);
                }

                if (!respostas.containsKey("401")) {
                    respostas.addApiResponse("401", new ApiResponse()
                            .description("Não autenticado (token JWT ausente, inválido ou expirado)"));
                }
                if (!respostas.containsKey("403") && !"/auth/me".equals(caminho)) {
                    respostas.addApiResponse("403", new ApiResponse()
                            .description("Acesso não autorizado para o perfil do usuário autenticado"));
                }
            }));
        };
    }
}
