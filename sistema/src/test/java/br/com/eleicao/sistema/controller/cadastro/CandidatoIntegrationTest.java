package br.com.eleicao.sistema.controller.cadastro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("/candidatos")
class CandidatoIntegrationTest extends CadastrosIntegrationBase {

    private String candidatoJson(int numero, String login, long partidoId, long eleicaoId) {
        return """
                {"nome":"Joao","numero":%d,"cargo":"GOVERNADOR","partidoId":%d,"eleicaoId":%d,"login":"%s","senha":"abc"}
                """.formatted(numero, partidoId, eleicaoId, login);
    }

    @Test
    @DisplayName("cadastra candidato válido e retorna 201")
    void cadastrar_dadosValidos() throws Exception {
        long partidoId = criarPartido("PAA");
        long eleicaoId = criarEleicao();

        postJson("/candidatos", candidatoJson(10, "joao", partidoId, eleicaoId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value(10))
                .andExpect(jsonPath("$.cargo").value("GOVERNADOR"))
                .andExpect(jsonPath("$.partidoSigla").value("PAA"));
    }

    @Test
    @DisplayName("rejeita número repetido no mesmo cargo e eleição com 409")
    void cadastrar_numeroRepetido() throws Exception {
        long partidoId = criarPartido("PAA");
        long eleicaoId = criarEleicao();
        postJson("/candidatos", candidatoJson(10, "joao", partidoId, eleicaoId))
                .andExpect(status().isCreated());

        postJson("/candidatos", candidatoJson(10, "outro", partidoId, eleicaoId))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("retorna 404 quando o partido não existe")
    void cadastrar_partidoInexistente() throws Exception {
        long eleicaoId = criarEleicao();

        postJson("/candidatos", candidatoJson(10, "joao", 99999L, eleicaoId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("lista só os candidatos do cargo e da eleição informados")
    void listarDisponiveis_filtraPorCargoEEleicao() throws Exception {
        long partidoId = criarPartido("PAA");
        long eleicaoId = criarEleicao();
        postJson("/candidatos", candidatoJson(10, "joao", partidoId, eleicaoId))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/candidatos")
                        .param("eleicaoId", String.valueOf(eleicaoId))
                        .param("cargo", "GOVERNADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/candidatos")
                        .param("eleicaoId", String.valueOf(eleicaoId))
                        .param("cargo", "SENADOR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}