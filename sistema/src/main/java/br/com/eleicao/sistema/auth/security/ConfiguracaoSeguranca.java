package br.com.eleicao.sistema.auth.security;

import br.com.eleicao.sistema.auth.service.ServicoJwt;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@Profile("!dev")
@RequiredArgsConstructor
public class ConfiguracaoSeguranca {

    private final ServicoJwt servicoJwt;
    private final RespostasSeguranca respostasSeguranca;

    @Bean
    public SecurityFilterChain cadeiaFiltrosSeguranca(HttpSecurity http) throws Exception {
        FiltroAutenticacaoJwt filtroJwt = new FiltroAutenticacaoJwt(servicoJwt);
        var getCandidatos = PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/candidatos");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(erro -> erro
                        .authenticationEntryPoint(respostasSeguranca.aoExigirAutenticacao())
                        .accessDeniedHandler(respostasSeguranca.aoNegarAcesso()))
                .authorizeHttpRequests(autenticacao -> autenticacao
                        // Rotas públicas e documentação OpenAPI/Swagger
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/error"
                        ).permitAll()

                        // Identidade do usuário autenticado
                        .requestMatchers(HttpMethod.GET, "/auth/me").authenticated()

                        // Gerenciamento de eleições: consulta para autenticados, escrita restrita ao CHEFE
                        .requestMatchers(HttpMethod.GET, "/eleicoes", "/eleicoes/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/eleicoes", "/eleicoes/**").hasRole("CHEFE")
                        .requestMatchers(HttpMethod.PUT, "/eleicoes", "/eleicoes/**").hasRole("CHEFE")
                        .requestMatchers(HttpMethod.PATCH, "/eleicoes", "/eleicoes/**").hasRole("CHEFE")
                        .requestMatchers(HttpMethod.DELETE, "/eleicoes", "/eleicoes/**").hasRole("CHEFE")

                        // Gerenciamento de eleitores: exclusivo do CHEFE
                        .requestMatchers("/eleitores", "/eleitores/**").hasRole("CHEFE")

                        // Eleitores e candidatos podem consultar candidatos disponíveis por eleição e cargo.
                        .requestMatchers(requisicao -> getCandidatos.matches(requisicao)
                                && StringUtils.hasText(requisicao.getParameter("eleicaoId"))
                                && StringUtils.hasText(requisicao.getParameter("cargo")))
                        .hasAnyRole("CHEFE", "ELEITOR", "CANDIDATO")
                        .requestMatchers("/candidatos", "/candidatos/**", "/partidos", "/partidos/**").hasRole("CHEFE")

                        // Urna de votação: eleitores e candidatos
                        .requestMatchers("/urna", "/urna/**").hasAnyRole("ELEITOR", "CANDIDATO")

                        // Acompanhamento e resultados: perfis autenticados (restrição por cargo no serviço)
                        .requestMatchers(
                                "/acompanhamento", "/acompanhamento/**",
                                "/resultados", "/resultados/**",
                                "/apuracao", "/apuracao/**"
                        ).hasAnyRole("CHEFE", "ELEITOR", "CANDIDATO")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
