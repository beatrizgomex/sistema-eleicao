package br.com.eleicao.sistema.controller;

import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.auth.service.ServicoJwt;
import br.com.eleicao.sistema.auth.service.UsuarioService;
import br.com.eleicao.sistema.shared.enums.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Segurança e proteção de rotas JWT")
class TesteIntegracaoSeguranca {

    @Autowired
    private MockMvc simuladorMvc;

    @Autowired
    private UsuarioService servicoUsuario;

    @Autowired
    private ServicoJwt servicoJwt;

    private Usuario chefe;
    private Usuario eleitor;
    private Usuario candidato;

    @BeforeEach
    void prepararUsuarios() {
        chefe = servicoUsuario.buscarPorLogin("chefe")
                .orElseGet(() -> servicoUsuario.criar("chefe_sec", "chefe123", Perfil.CHEFE));
        eleitor = servicoUsuario.criar("eleitor_sec", "senha123", Perfil.ELEITOR);
        candidato = servicoUsuario.criar("candidato_sec", "senha123", Perfil.CANDIDATO);
    }

    private String cabecalhoAutorizacao(Usuario usuario) {
        return "Bearer " + servicoJwt.gerarToken(usuario);
    }

    @Test
    @DisplayName("retorna 401 em JSON quando rota protegida é chamada sem token")
    void semTokenRetorna401Json() throws Exception {
        simuladorMvc.perform(get("/eleitores"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/eleitores"));
    }

    @Test
    @DisplayName("retorna 401 quando token JWT é inválido ou adulterado")
    void tokenInvalidoRetorna401() throws Exception {
        simuladorMvc.perform(get("/eleicoes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("retorna 401 quando token JWT está expirado")
    void tokenExpiradoRetorna401() throws Exception {
        Instant passado = Instant.now().minusSeconds(7200);
        String expirado = servicoJwt.gerarToken(eleitor, passado, passado.plusSeconds(60));

        simuladorMvc.perform(get("/eleicoes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expirado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("retorna 403 em JSON quando ELEITOR tenta cadastrar eleitor")
    void eleitorEmPostEleitoresRetorna403() throws Exception {
        simuladorMvc.perform(post("/eleitores")
                        .header(HttpHeaders.AUTHORIZATION, cabecalhoAutorizacao(eleitor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ana","documento":"999111","login":"ana_nova","senha":"123"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/eleitores"));
    }

    @Test
    @DisplayName("permite 201 quando CHEFE cadastra eleitor")
    void chefeEmPostEleitoresRetorna201() throws Exception {
        simuladorMvc.perform(post("/eleitores")
                        .header(HttpHeaders.AUTHORIZATION, cabecalhoAutorizacao(chefe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ana","documento":"999222","login":"ana_chefe","senha":"123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Ana"));
    }

    @Test
    @DisplayName("permite ao ELEITOR consultar candidatos por eleição e cargo")
    void eleitorConsultaCandidatosPorEleicaoECargo() throws Exception {
        simuladorMvc.perform(get("/candidatos")
                        .param("eleicaoId", "1")
                        .param("cargo", "PRESIDENTE")
                        .header(HttpHeaders.AUTHORIZATION, cabecalhoAutorizacao(eleitor)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("restringe listagem geral, filtros incompletos e consultas por ID ao CHEFE")
    void eleitorSemOsDoisFiltrosOuEmConsultaPorIdRecebe403() throws Exception {
        String autorizacao = cabecalhoAutorizacao(eleitor);

        simuladorMvc.perform(get("/candidatos").header(HttpHeaders.AUTHORIZATION, autorizacao))
                .andExpect(status().isForbidden());
        simuladorMvc.perform(get("/candidatos").param("eleicaoId", "1")
                        .header(HttpHeaders.AUTHORIZATION, autorizacao))
                .andExpect(status().isForbidden());
        simuladorMvc.perform(get("/candidatos").param("cargo", "PRESIDENTE")
                        .header(HttpHeaders.AUTHORIZATION, autorizacao))
                .andExpect(status().isForbidden());
        simuladorMvc.perform(get("/candidatos").param("eleicaoId", "1").param("cargo", " ")
                        .header(HttpHeaders.AUTHORIZATION, autorizacao))
                .andExpect(status().isForbidden());
        simuladorMvc.perform(get("/candidatos/1").param("eleicaoId", "1").param("cargo", "PRESIDENTE")
                        .header(HttpHeaders.AUTHORIZATION, autorizacao))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("restringe consultas de partidos ao CHEFE")
    void eleitorEmGetPartidosRecebe403() throws Exception {
        String autorizacao = cabecalhoAutorizacao(eleitor);

        simuladorMvc.perform(get("/partidos").header(HttpHeaders.AUTHORIZATION, autorizacao))
                .andExpect(status().isForbidden());
        simuladorMvc.perform(get("/partidos/1").header(HttpHeaders.AUTHORIZATION, autorizacao))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("permite ao CHEFE listar candidatos e partidos")
    void chefeListaCandidatosEPartidos() throws Exception {
        String autorizacao = cabecalhoAutorizacao(chefe);

        simuladorMvc.perform(get("/candidatos").header(HttpHeaders.AUTHORIZATION, autorizacao))
                .andExpect(status().isOk());
        simuladorMvc.perform(get("/partidos").header(HttpHeaders.AUTHORIZATION, autorizacao))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("retorna 403 quando CANDIDATO tenta listar candidatos")
    void candidatoEmGetCandidatosRetorna403() throws Exception {
        simuladorMvc.perform(get("/candidatos")
                        .header(HttpHeaders.AUTHORIZATION, cabecalhoAutorizacao(candidato)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("retorna 403 quando CHEFE tenta acessar POST /urna/votos")
    void chefeEmPostUrnaVotosRetorna403() throws Exception {
        simuladorMvc.perform(post("/urna/votos")
                        .header(HttpHeaders.AUTHORIZATION, cabecalhoAutorizacao(chefe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("permite acesso público sem token a /v3/api-docs")
    void v3ApiDocsPublicoSemToken() throws Exception {
        simuladorMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("retorna id, login e perfil do token em GET /auth/me")
    void consultaDoUsuarioRetornaDadosDoToken() throws Exception {
        simuladorMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, cabecalhoAutorizacao(candidato)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(candidato.getId()))
                .andExpect(jsonPath("$.login").value("candidato_sec"))
                .andExpect(jsonPath("$.perfil").value("CANDIDATO"));
    }

    @Test
    @DisplayName("preserva 404 de recurso inexistente sem transformar em 401")
    void recursoInexistentePreserva404() throws Exception {
        simuladorMvc.perform(get("/eleicoes/999999")
                        .header(HttpHeaders.AUTHORIZATION, cabecalhoAutorizacao(eleitor)))
                .andExpect(status().isNotFound());
    }
}
