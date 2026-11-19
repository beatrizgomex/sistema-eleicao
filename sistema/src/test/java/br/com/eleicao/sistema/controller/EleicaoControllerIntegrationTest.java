
package br.com.eleicao.sistema.eleicao.controller;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class EleicaoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void deveCadastrarEleicao() throws Exception {
        String json = """
                {
                  "data": "2026-11-20",
                  "turno": "PRIMEIRO",
                  "horaAbertura": "08:00:00",
                  "horaEncerramento": "17:00:00"
                }
                """;

        mockMvc.perform(post("/eleicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("AGENDADA"))
                .andExpect(jsonPath("$.turno").value("PRIMEIRO"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void deveListarEleicoes() throws Exception {
        mockMvc.perform(get("/eleicoes"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON));
    }


@Test
void deveBuscarEleicaoPorId() throws Exception {
    String json = """
            {
              "data": "2026-11-20",
              "turno": "PRIMEIRO",
              "horaAbertura": "08:00:00",
              "horaEncerramento": "17:00:00"
            }
            """;

    String resposta = mockMvc.perform(post("/eleicoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // Extrai o ID gerado no cadastro
    String id = resposta.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");

    mockMvc.perform(get("/eleicoes/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(Integer.parseInt(id)))
            .andExpect(jsonPath("$.situacao").value("AGENDADA"));
}




@Test
void deveAlterarEleicaoAgendada() throws Exception {
    String jsonCadastro = """
            {
              "data": "2026-11-20",
              "turno": "PRIMEIRO",
              "horaAbertura": "08:00:00",
              "horaEncerramento": "17:00:00"
            }
            """;

    String resposta = mockMvc.perform(post("/eleicoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonCadastro))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String id = resposta.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");

    String jsonAlteracao = """
            {
              "data": "2026-11-22",
              "turno": "SEGUNDO",
              "horaAbertura": "09:00:00",
              "horaEncerramento": "18:00:00"
            }
            """;

    mockMvc.perform(put("/eleicoes/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonAlteracao))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").value("2026-11-22"))
            .andExpect(jsonPath("$.turno").value("SEGUNDO"))
            .andExpect(jsonPath("$.situacao").value("AGENDADA"));
}





@Test
void deveAbrirEleicaoAgendada() throws Exception {
    LocalDate hoje = LocalDate.now();
    LocalTime agora = LocalTime.now();

    String jsonCadastro = String.format("""
            {
              "data": "%s",
              "turno": "PRIMEIRO",
              "horaAbertura": "%s",
              "horaEncerramento": "%s"
            }
            """,
            hoje,
            agora.minusMinutes(10),
            agora.plusMinutes(10));

    String resposta = mockMvc.perform(post("/eleicoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonCadastro))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String id = resposta.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");

    mockMvc.perform(patch("/eleicoes/" + id + "/abrir"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("ABERTA"));
}





@Test
void deveEncerrarEleicaoAberta() throws Exception {
    LocalDate hoje = LocalDate.now();
    LocalTime agora = LocalTime.now();

    String jsonCadastro = String.format("""
            {
              "data": "%s",
              "turno": "PRIMEIRO",
              "horaAbertura": "%s",
              "horaEncerramento": "%s"
            }
            """,
            hoje,
            agora.minusMinutes(10),
            agora.plusMinutes(10));

    // 1. Cadastra uma eleicao
    String resposta = mockMvc.perform(post("/eleicoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonCadastro))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String id = resposta.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");

    // 2. Abre a eleicao
    mockMvc.perform(patch("/eleicoes/" + id + "/abrir"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("ABERTA"));

    // 3. Encerra a eleicao
    mockMvc.perform(patch("/eleicoes/" + id + "/encerrar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("ENCERRADA"));

    // 4. Confirma que a situacao foi salva no banco
    mockMvc.perform(get("/eleicoes/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("ENCERRADA"));
}


@Test
void naoDeveEncerrarEleicaoAgendada() throws Exception {
    String jsonCadastro = """
            {
              "data": "2026-11-20",
              "turno": "PRIMEIRO",
              "horaAbertura": "08:00:00",
              "horaEncerramento": "17:00:00"
            }
            """;

    String resposta = mockMvc.perform(post("/eleicoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonCadastro))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String id = resposta.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");

    // Tenta encerrar sem abrir primeiro
    mockMvc.perform(patch("/eleicoes/" + id + "/encerrar"))
            .andExpect(status().isConflict());

    // Confirma que a eleicao continua AGENDADA
    mockMvc.perform(get("/eleicoes/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("AGENDADA"));
}




@Test
void deveRetornar404ParaEleicaoInexistente() throws Exception {
    mockMvc.perform(get("/eleicoes/999999"))
            .andExpect(status().isNotFound());
}




@Test
void naoDeveCadastrarEleicaoComHorarioInvalido() throws Exception {
    String jsonInvalido = """
            {
              "data": "2026-11-20",
              "turno": "PRIMEIRO",
              "horaAbertura": "17:00:00",
              "horaEncerramento": "08:00:00"
            }
            """;

    mockMvc.perform(post("/eleicoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonInvalido))
            .andExpect(status().isBadRequest());
}




@Test
void naoDeveAlterarEleicaoAberta() throws Exception {
    LocalDate hoje = LocalDate.now();
    LocalTime agora = LocalTime.now();

    String jsonCadastro = String.format("""
            {
              "data": "%s",
              "turno": "PRIMEIRO",
              "horaAbertura": "%s",
              "horaEncerramento": "%s"
            }
            """,
            hoje,
            agora.minusMinutes(10),
            agora.plusMinutes(10));

    // 1. Cadastra a eleicao
    String resposta = mockMvc.perform(post("/eleicoes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonCadastro))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String id = resposta.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");

    // 2. Abre a eleicao
    mockMvc.perform(patch("/eleicoes/" + id + "/abrir"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("ABERTA"));

    // 3. Tenta alterar uma eleicao aberta
    String jsonAlteracao = """
            {
              "data": "2026-11-25",
              "turno": "SEGUNDO",
              "horaAbertura": "09:00:00",
              "horaEncerramento": "18:00:00"
            }
            """;

    mockMvc.perform(put("/eleicoes/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonAlteracao))
            .andExpect(status().isConflict());

    // 4. Confirma que a eleicao continua aberta
    mockMvc.perform(get("/eleicoes/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("ABERTA"))
            .andExpect(jsonPath("$.turno").value("PRIMEIRO"));
}

}
