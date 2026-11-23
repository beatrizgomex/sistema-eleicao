package br.com.eleicao.sistema.apuracao.controller;

import br.com.eleicao.sistema.apuracao.dto.ApuracaoParcialResponse;
import br.com.eleicao.sistema.apuracao.dto.ResultadoCargoResponse;
import br.com.eleicao.sistema.apuracao.service.ApuracaoService;
import br.com.eleicao.sistema.shared.enums.Cargo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apuracao")
@RequiredArgsConstructor
@Tag(name = "Apuração", description = "Resultados parciais e finais da eleição")
public class ApuracaoController {

    private final ApuracaoService apuracaoService;

    @GetMapping("/parcial")
    @Operation(summary = "Apuração parcial",
            description = "Votos por candidato de um cargo, com brancos e nulos. Só disponível com a eleição "
                    + "ABERTA. CHEFE e ELEITOR veem todos os cargos; CANDIDATO vê somente o próprio cargo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Apuração parcial consultada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Candidato consultando outro cargo"),
            @ApiResponse(responseCode = "404", description = "Eleição não encontrada"),
            @ApiResponse(responseCode = "409", description = "Eleição não está aberta")
    })
    public ApuracaoParcialResponse parcial(@RequestParam Long eleicaoId, @RequestParam Cargo cargo) {
        return apuracaoService.parcial(eleicaoId, cargo);
    }

    @GetMapping("/resultado")
    @Operation(summary = "Resultado final",
            description = "Vencedor de cada cargo. Só fica disponível com a eleição ENCERRADA. Brancos e nulos "
                    + "não disputam a vitória; em empate no topo, vencedor fica nulo e empate é true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado consultado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "404", description = "Eleição não encontrada"),
            @ApiResponse(responseCode = "409", description = "Eleição não está encerrada")
    })
    public List<ResultadoCargoResponse> resultado(@RequestParam Long eleicaoId) {
        return apuracaoService.resultado(eleicaoId);
    }
}
