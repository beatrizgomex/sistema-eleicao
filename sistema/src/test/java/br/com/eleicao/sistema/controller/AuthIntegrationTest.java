package br.com.eleicao.sistema.controller;

import br.com.eleicao.sistema.auth.security.UsuarioAutenticado;
import br.com.eleicao.sistema.auth.service.JwtService;
import br.com.eleicao.sistema.auth.service.UsuarioService;
import br.com.eleicao.sistema.shared.enums.Perfil;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
@Transactional
@DisplayName("/auth")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtService jwtService;

    @Test
    @DisplayName("autentica chefe inicial com 200 e devolve token JWT válido")
    void loginChefeInicialDevolveTokenValido() throws Exception {
        String resposta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"chefe","senha":"chefe123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").isNotEmpty())
                .andExpect(jsonPath("$.perfil").value("CHEFE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(resposta, "$.token");
        UsuarioAutenticado autenticado = jwtService.validar(token);
        assertEquals("chefe", autenticado.login());
        assertEquals(Perfil.CHEFE, autenticado.perfil());
    }

    @Test
    @DisplayName("autentica eleitor cadastrado e devolve perfil ELEITOR")
    void loginEleitorDevolvePerfilEleitor() throws Exception {
        usuarioService.criar("eleitor_auth", "segredo", Perfil.ELEITOR);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"eleitor_auth","senha":"segredo"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.perfil").value("ELEITOR"));
    }

    @Test
    @DisplayName("rejeita senha incorreta com 401")
    void loginSenhaIncorretaRetorna401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"chefe","senha":"errada"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejeita login inexistente com 401")
    void loginInexistenteRetorna401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"inexistente","senha":"qualquer"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejeita campos em branco com 400")
    void loginCamposEmBrancoRetorna400() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"","senha":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
