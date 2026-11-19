package br.com.eleicao.sistema.controller.cadastro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("/eleitores")
class EleitorIntegrationTest extends CadastrosIntegrationBase {

    private String eleitorJson(String nome, String documento, String login) {
        return """
                {"nome":"%s","documento":"%s","login":"%s","senha":"abc"}
                """.formatted(nome, documento, login);
    }

    @Test
    @DisplayName("cadastra eleitor válido, retorna 201 e não expõe a senha")
    void cadastrar_dadosValidosa() throws Exception {
        postJson("/eleitores", eleitorJson("Maria", "111", "maria"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Maria"))
                .andExpect(jsonPath("$.documento").value("111"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    @DisplayName("rejeita documento repetido com 409")
    void cadastrar_documentoRepetid() throws Exception {
        postJson("/eleitores", eleitorJson("Maria", "111", "maria"))
                .andExpect(status().isCreated());

        postJson("/eleitores", eleitorJson("Outra", "111", "outra"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("rejeita nome em branco com 400")
    void cadastrar_nomeEmBranco() throws Exception {
        postJson("/eleitores", eleitorJson("", "111", "maria"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("exclui eleitor com 204 e depois retorna 404 ao buscá-lo")
    void excluir_existente_retorna204EDepois404() throws Exception {
        long id = idDaResposta(postJson("/eleitores", eleitorJson("Maria", "111", "maria"))
                .andExpect(status().isCreated()));

        mockMvc.perform(delete("/eleitores/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/eleitores/" + id)).andExpect(status().isNotFound());
    }
}