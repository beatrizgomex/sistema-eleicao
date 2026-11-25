package br.com.eleicao.sistema.controller;

import com.jayway.jsonpath.JsonPath;
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

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Fluxo ponta a ponta da eleição com candidato votando autenticado via JWT")
class FluxoCompletoEleicaoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String autenticarViaLogin(String login, String senha, String perfilEsperado) throws Exception {
        String resposta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"%s","senha":"%s"}
                                """.formatted(login, senha)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value(perfilEsperado))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return "Bearer " + JsonPath.read(resposta, "$.token");
    }

    @Test
    @DisplayName("executa fluxo completo: chefe prepara eleição, candidato e eleitor logam e votam, e apuração contabiliza ambos")
    void fluxoCompletoComCandidatoVotandoAutenticado() throws Exception {
        // 1. Chefe faz login real via POST /auth/login
        String tokenChefe = autenticarViaLogin("chefe", "chefe123", "CHEFE");

        // 2. Chefe cadastra eleição dentro da janela de horário atual
        LocalDate hoje = LocalDate.now();
        LocalTime abertura = LocalTime.now().minusMinutes(5).withNano(0);
        LocalTime encerramento = LocalTime.now().plusMinutes(20).withNano(0);
        // Garante janela válida mesmo próximo da meia-noite
        if (!encerramento.isAfter(abertura)) {
            abertura = LocalTime.of(0, 0);
            encerramento = LocalTime.of(23, 59, 59);
        }

        String eleicaoResp = mockMvc.perform(post("/eleicoes")
                        .header(HttpHeaders.AUTHORIZATION, tokenChefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "data": "%s",
                                  "turno": "PRIMEIRO",
                                  "horaAbertura": "%s",
                                  "horaEncerramento": "%s"
                                }
                                """.formatted(hoje, abertura, encerramento)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.situacao").value("AGENDADA"))
                .andReturn().getResponse().getContentAsString();
        long eleicaoId = ((Number) JsonPath.read(eleicaoResp, "$.id")).longValue();

        // 3. Chefe cadastra partido, candidatos e eleitor via API
        String partidoResp = mockMvc.perform(post("/partidos")
                        .header(HttpHeaders.AUTHORIZATION, tokenChefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sigla":"PFLX","nome":"Partido Fluxo"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long partidoId = ((Number) JsonPath.read(partidoResp, "$.id")).longValue();

        String candGovResp = mockMvc.perform(post("/candidatos")
                        .header(HttpHeaders.AUTHORIZATION, tokenChefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Ana Governadora",
                                  "numero": 10,
                                  "cargo": "GOVERNADOR",
                                  "partidoId": %d,
                                  "eleicaoId": %d,
                                  "login": "ana_cand",
                                  "senha": "senhaCand10"
                                }
                                """.formatted(partidoId, eleicaoId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long candidatoAnaId = ((Number) JsonPath.read(candGovResp, "$.id")).longValue();

        mockMvc.perform(post("/candidatos")
                        .header(HttpHeaders.AUTHORIZATION, tokenChefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Carlos Presidente",
                                  "numero": 20,
                                  "cargo": "PRESIDENTE",
                                  "partidoId": %d,
                                  "eleicaoId": %d,
                                  "login": "carlos_cand",
                                  "senha": "senhaCand20"
                                }
                                """.formatted(partidoId, eleicaoId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/eleitores")
                        .header(HttpHeaders.AUTHORIZATION, tokenChefe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nome": "Bruno Eleitor",
                                  "documento": "DOC-FLUXO-01",
                                  "login": "bruno_eleitor",
                                  "senha": "senhaEleitor"
                                }
                                """))
                .andExpect(status().isCreated());

        // 4. Chefe abre a eleição
        mockMvc.perform(patch("/eleicoes/" + eleicaoId + "/abrir")
                        .header(HttpHeaders.AUTHORIZATION, tokenChefe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ABERTA"));

        // 5. CANDIDATA faz login real no próprio perfil via POST /auth/login
        String tokenCandidata = autenticarViaLogin("ana_cand", "senhaCand10", "CANDIDATO");

        // 6. Candidata confere sua identidade (/auth/me), eleições, candidatos disponíveis e cédula da urna
        mockMvc.perform(get("/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, tokenCandidata))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("ana_cand"))
                .andExpect(jsonPath("$.perfil").value("CANDIDATO"));

        mockMvc.perform(get("/eleicoes/" + eleicaoId)
                        .header(HttpHeaders.AUTHORIZATION, tokenCandidata))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ABERTA"));

        mockMvc.perform(get("/candidatos")
                        .param("eleicaoId", String.valueOf(eleicaoId))
                        .param("cargo", "GOVERNADOR")
                        .header(HttpHeaders.AUTHORIZATION, tokenCandidata))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Ana Governadora"));

        mockMvc.perform(get("/urna/cedula")
                        .param("eleicaoId", String.valueOf(eleicaoId))
                        .header(HttpHeaders.AUTHORIZATION, tokenCandidata))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // 7. Candidata registra seu voto na urna e recebe comprovante
        String comprovanteCandResp = mockMvc.perform(post("/urna/votos")
                        .header(HttpHeaders.AUTHORIZATION, tokenCandidata)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eleicaoId": %d,
                                  "votos": [
                                    {"cargo": "GOVERNADOR", "tipo": "CANDIDATO", "numero": 10},
                                    {"cargo": "PRESIDENTE", "tipo": "CANDIDATO", "numero": 20}
                                  ]
                                }
                                """.formatted(eleicaoId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").isNotEmpty())
                .andExpect(jsonPath("$.eleicaoId").value(eleicaoId))
                .andReturn().getResponse().getContentAsString();
        String codigoComprovanteCand = JsonPath.read(comprovanteCandResp, "$.codigo");

        // Segundo voto da mesma candidata na mesma eleição é barrado com 409
        mockMvc.perform(post("/urna/votos")
                        .header(HttpHeaders.AUTHORIZATION, tokenCandidata)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eleicaoId": %d,
                                  "votos": [
                                    {"cargo": "GOVERNADOR", "tipo": "BRANCO"},
                                    {"cargo": "PRESIDENTE", "tipo": "BRANCO"}
                                  ]
                                }
                                """.formatted(eleicaoId)))
                .andExpect(status().isConflict());

        // Candidata consulta seu comprovante de votação
        mockMvc.perform(get("/urna/comprovante")
                        .param("eleicaoId", String.valueOf(eleicaoId))
                        .header(HttpHeaders.AUTHORIZATION, tokenCandidata))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value(codigoComprovanteCand));

        // Candidata consulta apuração parcial do próprio cargo (200) e é impedida em outro cargo (403)
        mockMvc.perform(get("/apuracao/parcial")
                        .param("eleicaoId", String.valueOf(eleicaoId))
                        .param("cargo", "GOVERNADOR")
                        .header(HttpHeaders.AUTHORIZATION, tokenCandidata))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidatos[0].votos").value(1));

        mockMvc.perform(get("/apuracao/parcial")
                        .param("eleicaoId", String.valueOf(eleicaoId))
                        .param("cargo", "PRESIDENTE")
                        .header(HttpHeaders.AUTHORIZATION, tokenCandidata))
                .andExpect(status().isForbidden());

        // 8. Eleitor comum também loga e vota normalmente
        String tokenEleitor = autenticarViaLogin("bruno_eleitor", "senhaEleitor", "ELEITOR");
        mockMvc.perform(post("/urna/votos")
                        .header(HttpHeaders.AUTHORIZATION, tokenEleitor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "eleicaoId": %d,
                                  "votos": [
                                    {"cargo": "GOVERNADOR", "tipo": "CANDIDATO", "numero": 10},
                                    {"cargo": "PRESIDENTE", "tipo": "BRANCO"}
                                  ]
                                }
                                """.formatted(eleicaoId)))
                .andExpect(status().isCreated());

        // 9. Chefe encerra a eleição e não consegue excluir candidata que participou/recebeu votos
        mockMvc.perform(patch("/eleicoes/" + eleicaoId + "/encerrar")
                        .header(HttpHeaders.AUTHORIZATION, tokenChefe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ENCERRADA"));

        mockMvc.perform(delete("/candidatos/" + candidatoAnaId)
                        .header(HttpHeaders.AUTHORIZATION, tokenChefe))
                .andExpect(status().isConflict());

        // 10. Resultado final reflete exatamente os votos da candidata + eleitor
        mockMvc.perform(get("/apuracao/resultado")
                        .param("eleicaoId", String.valueOf(eleicaoId))
                        .header(HttpHeaders.AUTHORIZATION, tokenCandidata))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cargo").value("GOVERNADOR"))
                .andExpect(jsonPath("$[0].vencedor.nome").value("Ana Governadora"))
                .andExpect(jsonPath("$[0].vencedor.votos").value(2))
                .andExpect(jsonPath("$[1].cargo").value("PRESIDENTE"))
                .andExpect(jsonPath("$[1].vencedor.nome").value("Carlos Presidente"))
                .andExpect(jsonPath("$[1].vencedor.votos").value(1))
                .andExpect(jsonPath("$[1].brancos").value(1));
    }
}
