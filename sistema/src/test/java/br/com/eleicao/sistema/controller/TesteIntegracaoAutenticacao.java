package br.com.eleicao.sistema.controller;

import br.com.eleicao.sistema.auth.security.UsuarioAutenticado;
import br.com.eleicao.sistema.auth.service.ServicoJwt;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
@Transactional
@DisplayName("/auth")
class TesteIntegracaoAutenticacao {

    @Autowired
    private MockMvc simuladorMvc;

    @Autowired
    private UsuarioService servicoUsuario;

    @Autowired
    private ServicoJwt servicoJwt;

    @Test
    @DisplayName("autentica chefe inicial com 200 e devolve token JWT válido")
    void loginChefeInicialDevolveTokenValido() throws Exception {
        String resposta = simuladorMvc.perform(post("/auth/login")
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
        UsuarioAutenticado autenticado = servicoJwt.validar(token);
        assertEquals("chefe", autenticado.login());
        assertEquals(Perfil.CHEFE, autenticado.perfil());
    }

    @Test
    @DisplayName("autentica eleitor cadastrado e devolve perfil ELEITOR")
    void loginEleitorDevolvePerfilEleitor() throws Exception {
        servicoUsuario.criar("eleitor_auth", "segredo", Perfil.ELEITOR);

        simuladorMvc.perform(post("/auth/login")
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
        simuladorMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"chefe","senha":"errada"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejeita login inexistente com 401")
    void loginInexistenteRetorna401() throws Exception {
        simuladorMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"inexistente","senha":"qualquer"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejeita campos em branco com 400")
    void loginCamposEmBrancoRetorna400() throws Exception {
        simuladorMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"","senha":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("bloqueia usuário após 5 tentativas falhas e exibe apenas mensagem para tentar mais tarde")
    void bloqueiaUsuarioAposCincoTentativasFalhasSemExibirTempo() throws Exception {
        servicoUsuario.criar("candidato_bloqueio", "senhaCorreta", Perfil.CANDIDATO);

        for (int tentativa = 1; tentativa <= 4; tentativa++) {
            simuladorMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"login":"candidato_bloqueio","senha":"errada"}
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Login ou senha inválidos"));
        }

        String corpoQuintaFalha = simuladorMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"candidato_bloqueio","senha":"errada"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Tente novamente mais tarde"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(corpoQuintaFalha.toLowerCase().contains("minuto"));
        assertFalse(corpoQuintaFalha.contains("bloqueadoAte"));

        // Mesmo informando a senha correta durante o período de bloqueio, o acesso permanece negado
        simuladorMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"candidato_bloqueio","senha":"senhaCorreta"}
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Tente novamente mais tarde"))
                .andExpect(jsonPath("$.token").doesNotExist());

        var usuarioPersistido = servicoUsuario.buscarPorLogin("candidato_bloqueio").orElseThrow();
        assertEquals(5, usuarioPersistido.getTentativasFalhas());
        assertNotNull(usuarioPersistido.getBloqueadoAte());
    }

    @Test
    @DisplayName("zera contagem de falhas ao autenticar com sucesso antes da quinta tentativa")
    void zeraFalhasAoAutenticarAntesDoLimite() throws Exception {
        servicoUsuario.criar("eleitor_reset", "senhaValida", Perfil.ELEITOR);

        for (int tentativa = 1; tentativa <= 4; tentativa++) {
            simuladorMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"login":"eleitor_reset","senha":"incorreta"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        simuladorMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"eleitor_reset","senha":"senhaValida"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // Após sucesso, mais 4 falhas retornam 401 (não bloqueiam na primeira nem na quarta)
        for (int tentativa = 1; tentativa <= 4; tentativa++) {
            simuladorMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"login":"eleitor_reset","senha":"incorreta"}
                                    """))
                    .andExpect(status().isUnauthorized());
        }
    }
}
