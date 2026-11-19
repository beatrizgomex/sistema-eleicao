package br.com.eleicao.sistema.controller.cadastro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("POST/GET /partidos")
class PartidoIntegrationTest extends CadastrosIntegrationBase {

    @Test
    @DisplayName("cadastra partido válido e retorna 201")
    void cadastrar_dadosValidos() throws Exception {
        postJson("/partidos", partidoJson("PAA", "Partido A"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sigla").value("PAA"));
    }

    @Test
    @DisplayName("rejeita sigla repetida com 409")
    void cadastrar_siglaRepetida() throws Exception {
        criarPartido("PAA");

        postJson("/partidos", partidoJson("PAA", "Outro nome"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("rejeita nome em branco com 400")
    void cadastrar_nomeEmBranco() throws Exception {
        postJson("/partidos", partidoJson("PAA", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("retorna 404 ao buscar partido inexistente")
    void buscarPorId_inexistente() throws Exception {
        mockMvc.perform(get("/partidos/99999"))
                .andExpect(status().isNotFound());
    }
}