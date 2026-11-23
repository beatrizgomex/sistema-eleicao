package br.com.eleicao.sistema.controller;

import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.candidato.model.Candidato;
import br.com.eleicao.sistema.candidato.model.Partido;
import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.shared.enums.Cargo;
import br.com.eleicao.sistema.shared.enums.Perfil;
import br.com.eleicao.sistema.shared.enums.SituacaoEleicao;
import br.com.eleicao.sistema.shared.enums.TipoVoto;
import br.com.eleicao.sistema.shared.enums.Turno;
import br.com.eleicao.sistema.votacao.model.Voto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
@Transactional
@DisplayName("/apuracao")
class ApuracaoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @PersistenceContext
    private EntityManager entityManager;

    private Eleicao eleicao;
    private Candidato ana;
    private Candidato bruno;
    private Long chefeId;

    @BeforeEach
    void prepararEleicao() {
        eleicao = new Eleicao();
        eleicao.setData(LocalDate.now());
        eleicao.setTurno(Turno.PRIMEIRO);
        eleicao.setHoraAbertura(LocalTime.of(8, 0));
        eleicao.setHoraEncerramento(LocalTime.of(17, 0));
        eleicao.setSituacao(SituacaoEleicao.ABERTA);
        entityManager.persist(eleicao);

        Partido partido = new Partido();
        partido.setSigla("APU");
        partido.setNome("Partido da Apuração");
        entityManager.persist(partido);

        ana = candidato(partido, "Ana", 10, "ana");
        bruno = candidato(partido, "Bruno", 20, "bruno");

        Usuario chefe = usuario("chefe_apuracao", Perfil.CHEFE);
        entityManager.persist(chefe);
        chefeId = chefe.getId();
        entityManager.flush();
    }

    @Test
    @DisplayName("parcial traz candidato sem votos, brancos e nulos")
    void parcialTrazCandidatoSemVotosEBrancosENulos() throws Exception {
        voto(TipoVoto.CANDIDATO, ana);
        voto(TipoVoto.CANDIDATO, ana);
        voto(TipoVoto.BRANCO, null);
        voto(TipoVoto.NULO, null);
        voto(TipoVoto.NULO, null);
        voto(TipoVoto.NULO, null);

        consultarParcial(chefeId, Perfil.CHEFE, Cargo.GOVERNADOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("GOVERNADOR"))
                .andExpect(jsonPath("$.candidatos.length()").value(2))
                .andExpect(jsonPath("$.candidatos[0].nome").value("Ana"))
                .andExpect(jsonPath("$.candidatos[0].votos").value(2))
                .andExpect(jsonPath("$.candidatos[1].nome").value("Bruno"))
                .andExpect(jsonPath("$.candidatos[1].votos").value(0))
                .andExpect(jsonPath("$.brancos").value(1))
                .andExpect(jsonPath("$.nulos").value(3));
    }

    @Test
    @DisplayName("candidato só consulta a apuração do próprio cargo")
    void candidatoSoVeOProprioCargo() throws Exception {
        Long anaUsuarioId = ana.getUsuario().getId();

        consultarParcial(anaUsuarioId, Perfil.CANDIDATO, Cargo.GOVERNADOR)
                .andExpect(status().isOk());

        consultarParcial(anaUsuarioId, Perfil.CANDIDATO, Cargo.SENADOR)
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("parcial sem login retorna 401")
    void parcialSemLoginRetorna401() throws Exception {
        mockMvc.perform(get("/apuracao/parcial")
                        .param("eleicaoId", eleicao.getId().toString())
                        .param("cargo", "GOVERNADOR"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("parcial funciona com a eleição ABERTA")
    void parcialComEleicaoAbertaRetorna200() throws Exception {
        consultarParcial(chefeId, Perfil.CHEFE, Cargo.GOVERNADOR)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("parcial retorna 409 com eleição AGENDADA")
    void parcialRecusaEleicaoAgendada() throws Exception {
        eleicao.setSituacao(SituacaoEleicao.AGENDADA);

        consultarParcial(chefeId, Perfil.CHEFE, Cargo.GOVERNADOR)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("parcial retorna 409 com eleição ENCERRADA")
    void parcialRecusaEleicaoEncerrada() throws Exception {
        encerrarEleicao();

        consultarParcial(chefeId, Perfil.CHEFE, Cargo.GOVERNADOR)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CHEFE consulta a parcial de qualquer cargo")
    void chefeConsultaParcial() throws Exception {
        consultarParcial(chefeId, Perfil.CHEFE, Cargo.SENADOR)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ELEITOR consulta a parcial de qualquer cargo")
    void eleitorConsultaParcial() throws Exception {
        Usuario eleitor = usuario("eleitor", Perfil.ELEITOR);
        entityManager.persist(eleitor);

        consultarParcial(eleitor.getId(), Perfil.ELEITOR, Cargo.SENADOR)
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("parcial retorna 404 para eleição inexistente")
    void parcialEleicaoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/apuracao/parcial")
                        .param("eleicaoId", "-1")
                        .param("cargo", "GOVERNADOR")
                        .header("X-Usuario-Id", chefeId)
                        .header("X-Perfil", Perfil.CHEFE.name()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("resultado retorna 404 para eleição inexistente")
    void resultadoEleicaoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/apuracao/resultado")
                        .param("eleicaoId", "-1")
                        .header("X-Usuario-Id", chefeId)
                        .header("X-Perfil", Perfil.CHEFE.name()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("resultado retorna 409 enquanto a eleição não está encerrada")
    void resultadoExigeEleicaoEncerrada() throws Exception {
        mockMvc.perform(requisicaoResultado(chefeId, Perfil.CHEFE))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("resultado escolhe o vencedor sem contar brancos e nulos")
    void resultadoIgnoraBrancoENulo() throws Exception {
        encerrarEleicao();
        voto(TipoVoto.CANDIDATO, ana);
        for (int i = 0; i < 5; i++) {
            voto(TipoVoto.BRANCO, null);
            voto(TipoVoto.NULO, null);
        }

        mockMvc.perform(requisicaoResultado(chefeId, Perfil.CHEFE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cargo").value("GOVERNADOR"))
                .andExpect(jsonPath("$[0].vencedor.nome").value("Ana"))
                .andExpect(jsonPath("$[0].vencedor.votos").value(1))
                .andExpect(jsonPath("$[0].empate").value(false))
                .andExpect(jsonPath("$[0].brancos").value(5))
                .andExpect(jsonPath("$[0].nulos").value(5));
    }

    @Test
    @DisplayName("resultado marca empate quando há dois candidatos no topo")
    void resultadoMarcaEmpate() throws Exception {
        encerrarEleicao();
        voto(TipoVoto.CANDIDATO, ana);
        voto(TipoVoto.CANDIDATO, ana);
        voto(TipoVoto.CANDIDATO, bruno);
        voto(TipoVoto.CANDIDATO, bruno);

        mockMvc.perform(requisicaoResultado(chefeId, Perfil.CHEFE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].empate").value(true))
                .andExpect(jsonPath("$[0].vencedor").isEmpty());
    }

    private void encerrarEleicao() {
        eleicao.setSituacao(SituacaoEleicao.ENCERRADA);
    }

    private ResultActions consultarParcial(Long usuarioId, Perfil perfil, Cargo cargo) throws Exception {
        return mockMvc.perform(get("/apuracao/parcial")
                .param("eleicaoId", eleicao.getId().toString())
                .param("cargo", cargo.name())
                .header("X-Usuario-Id", usuarioId)
                .header("X-Perfil", perfil.name()));
    }

    private MockHttpServletRequestBuilder requisicaoResultado(Long usuarioId, Perfil perfil) {
        return get("/apuracao/resultado")
                .param("eleicaoId", eleicao.getId().toString())
                .header("X-Usuario-Id", usuarioId)
                .header("X-Perfil", perfil.name());
    }

    private void voto(TipoVoto tipo, Candidato candidato) {
        Voto voto = new Voto();
        voto.setEleicao(eleicao);
        voto.setCargo(Cargo.GOVERNADOR);
        voto.setTipo(tipo);
        voto.setCandidato(candidato);
        entityManager.persist(voto);
    }

    private Candidato candidato(Partido partido, String nome, int numero, String login) {
        Candidato candidato = new Candidato();
        candidato.setNome(nome);
        candidato.setNumero(numero);
        candidato.setCargo(Cargo.GOVERNADOR);
        candidato.setPartido(partido);
        candidato.setEleicao(eleicao);
        candidato.setUsuario(usuario(login, Perfil.CANDIDATO));
        entityManager.persist(candidato);
        return candidato;
    }

    private Usuario usuario(String login, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenha("x");
        usuario.setPerfil(perfil);
        entityManager.persist(usuario);
        return usuario;
    }
}
