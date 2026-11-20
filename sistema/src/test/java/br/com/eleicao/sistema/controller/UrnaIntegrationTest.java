package br.com.eleicao.sistema.controller;

import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.candidato.model.Candidato;
import br.com.eleicao.sistema.candidato.model.Partido;
import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.eleitor.model.Eleitor;
import br.com.eleicao.sistema.shared.enums.Cargo;
import br.com.eleicao.sistema.shared.enums.Perfil;
import br.com.eleicao.sistema.shared.enums.SituacaoEleicao;
import br.com.eleicao.sistema.shared.enums.Turno;
import br.com.eleicao.sistema.votacao.repository.VotoRepository;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
@Transactional
@DisplayName("/urna")
class UrnaIntegrationTest {

    private static final String CEDULA_VALIDA = """
            [{"cargo":"GOVERNADOR","tipo":"CANDIDATO","numero":10},{"cargo":"PRESIDENTE","tipo":"BRANCO"}]""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VotoRepository votoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Eleicao eleicao;
    private Long eleitorUsuarioId;

    @BeforeEach
    void prepararEleicaoAberta() {
        eleicao = new Eleicao();
        eleicao.setData(LocalDate.now());
        eleicao.setTurno(Turno.PRIMEIRO);
        eleicao.setHoraAbertura(LocalTime.of(8, 0));
        eleicao.setHoraEncerramento(LocalTime.of(17, 0));
        eleicao.setSituacao(SituacaoEleicao.ABERTA);
        entityManager.persist(eleicao);

        Partido partido = new Partido();
        partido.setSigla("PUR");
        partido.setNome("Partido da Urna");
        entityManager.persist(partido);

        candidato(partido, 10, Cargo.GOVERNADOR, "gov10");
        candidato(partido, 20, Cargo.GOVERNADOR, "gov20");
        candidato(partido, 13, Cargo.PRESIDENTE, "pres13");

        Eleitor eleitor = new Eleitor();
        eleitor.setNome("Eleitor");
        eleitor.setDocumento("123");
        eleitor.setUsuario(usuario("eleitor", Perfil.ELEITOR));
        entityManager.persist(eleitor);
        entityManager.flush();
        eleitorUsuarioId = eleitor.getUsuario().getId();
    }

    private Usuario usuario(String login, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenha("x");
        usuario.setPerfil(perfil);
        return usuario;
    }

    private void candidato(Partido partido, int numero, Cargo cargo, String login) {
        Candidato candidato = new Candidato();
        candidato.setNome("Candidato " + numero);
        candidato.setNumero(numero);
        candidato.setCargo(cargo);
        candidato.setPartido(partido);
        candidato.setEleicao(eleicao);
        candidato.setUsuario(usuario(login, Perfil.CANDIDATO));
        entityManager.persist(candidato);
    }

    private ResultActions votar(Long usuarioId, Perfil perfil, String votos) throws Exception {
        MockHttpServletRequestBuilder requisicao = post("/urna/votos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"eleicaoId":%d,"votos":%s}
                        """.formatted(eleicao.getId(), votos));
        if (usuarioId != null) {
            requisicao.header("X-Usuario-Id", usuarioId).header("X-Perfil", perfil.name());
        }
        return mockMvc.perform(requisicao);
    }

    private ResultActions votarComoEleitor(String votos) throws Exception {
        return votar(eleitorUsuarioId, Perfil.ELEITOR, votos);
    }

    @Test
    @DisplayName("monta a cédula com os cargos em disputa e seus candidatos")
    void cedula_cargosEmDisputa() throws Exception {
        mockMvc.perform(get("/urna/cedula").param("eleicaoId", String.valueOf(eleicao.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].cargo").value("GOVERNADOR"))
                .andExpect(jsonPath("$[0].candidatos.length()").value(2))
                .andExpect(jsonPath("$[1].cargo").value("PRESIDENTE"))
                .andExpect(jsonPath("$[1].descricao").value("Presidente da República"));
    }

    @Test
    @DisplayName("vota, devolve só o comprovante e permite consultá-lo depois")
    void votar_cedulaValida() throws Exception {
        String corpo = votarComoEleitor(CEDULA_VALIDA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$.eleicaoId").value(eleicao.getId()))
                .andExpect(jsonPath("$.emitidoEm").exists())
                .andReturn().getResponse().getContentAsString();
        String codigo = JsonPath.read(corpo, "$.codigo");

        assertEquals(2, votoRepository.count());

        mockMvc.perform(get("/urna/comprovante")
                        .param("eleicaoId", String.valueOf(eleicao.getId()))
                        .header("X-Usuario-Id", eleitorUsuarioId)
                        .header("X-Perfil", "ELEITOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigo));
    }

    @Test
    @DisplayName("aceita voto nulo em todos os cargos")
    void votar_nulo() throws Exception {
        votarComoEleitor("""
                [{"cargo":"GOVERNADOR","tipo":"NULO"},{"cargo":"PRESIDENTE","tipo":"NULO"}]""")
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("rejeita o segundo voto do mesmo eleitor com 409")
    void votar_duasVezes() throws Exception {
        votarComoEleitor(CEDULA_VALIDA).andExpect(status().isCreated());
        votarComoEleitor(CEDULA_VALIDA).andExpect(status().isConflict());

        assertEquals(2, votoRepository.count());
    }

    @Test
    @DisplayName("rejeita voto em eleição que não está aberta com 409")
    void votar_eleicaoAgendada() throws Exception {
        eleicao.setSituacao(SituacaoEleicao.AGENDADA);
        entityManager.flush();

        votarComoEleitor(CEDULA_VALIDA).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("rejeita voto sem usuário logado com 401")
    void votar_semLogin() throws Exception {
        votar(null, null, CEDULA_VALIDA).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("rejeita voto de quem não é eleitor com 403")
    void votar_perfilChefe() throws Exception {
        votar(eleitorUsuarioId, Perfil.CHEFE, CEDULA_VALIDA).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("rejeita cédula incompleta com 400 e não grava nada")
    void votar_cedulaIncompleta() throws Exception {
        votarComoEleitor("""
                [{"cargo":"GOVERNADOR","tipo":"BRANCO"}]""")
                .andExpect(status().isBadRequest());

        assertEquals(0, votoRepository.count());
    }

    @Test
    @DisplayName("rejeita número sem candidato com 404")
    void votar_numeroInexistente() throws Exception {
        votarComoEleitor("""
                [{"cargo":"GOVERNADOR","tipo":"CANDIDATO","numero":99},{"cargo":"PRESIDENTE","tipo":"BRANCO"}]""")
                .andExpect(status().isNotFound());
    }
}
