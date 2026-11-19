package br.com.eleicao.sistema.controller.cadastro;
import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.shared.enums.Turno;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
@Transactional
abstract class CadastrosIntegrationBase {

    @Autowired
    protected MockMvc mockMvc;

    @PersistenceContext
    protected EntityManager entityManager;

    protected ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected long idDaResposta(ResultActions resultado) throws Exception {
        String corpo = resultado.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    protected long criarPartido(String sigla) throws Exception {
        return idDaResposta(postJson("/partidos", partidoJson(sigla, "Partido " + sigla))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().isCreated()));
    }

    protected String partidoJson(String sigla, String nome) {
        return """
                {"sigla":"%s","nome":"%s"}
                """.formatted(sigla, nome);
    }

    protected long criarEleicao() {
        Eleicao eleicao = new Eleicao();
        eleicao.setData(LocalDate.now().plusDays(30));
        eleicao.setTurno(Turno.values()[0]);
        eleicao.setHoraAbertura(LocalTime.of(8, 0));
        eleicao.setHoraEncerramento(LocalTime.of(17, 0));
        entityManager.persist(eleicao);
        entityManager.flush();
        return eleicao.getId();
    }
}