package br.com.eleicao.sistema.votacao.controller;

import br.com.eleicao.sistema.votacao.dto.CargoNaCedulaResponse;
import br.com.eleicao.sistema.votacao.dto.CedulaRequest;
import br.com.eleicao.sistema.votacao.dto.ComprovanteResponse;
import br.com.eleicao.sistema.votacao.service.UrnaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/urna")
@RequiredArgsConstructor
@Tag(name = "Urna", description = "Votação secreta de eleitores e candidatos, com emissão de comprovante")
public class UrnaController {

    private final UrnaService urnaService;

    @GetMapping("/cedula")
    @Operation(summary = "Montar cédula",
            description = "Lista os cargos em disputa na eleição e os candidatos de cada um.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cédula montada"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão para votar"),
            @ApiResponse(responseCode = "404", description = "Eleição ou votante não encontrado")
    })
    public List<CargoNaCedulaResponse> cedula(@RequestParam Long eleicaoId) {
        return urnaService.montarCedula(eleicaoId);
    }

    @PostMapping("/votos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Votar",
            description = "Registra a cédula do votante logado, com um voto por cargo em disputa "
                    + "(CANDIDATO com número, BRANCO ou NULO), e devolve o comprovante. "
                    + "O voto não guarda quem votou.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Voto registrado"),
            @ApiResponse(responseCode = "400", description = "Cédula incompleta, cargo repetido ou número indevido"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão para votar"),
            @ApiResponse(responseCode = "404", description = "Eleição, votante ou candidato não encontrado"),
            @ApiResponse(responseCode = "409", description = "Eleição não está aberta ou o votante já votou")
    })
    public ComprovanteResponse votar(@Valid @RequestBody CedulaRequest cedula) {
        return urnaService.votar(cedula);
    }

    @GetMapping("/comprovante")
    @Operation(summary = "Consultar comprovante",
            description = "Devolve o comprovante de votação do votante logado na eleição informada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comprovante encontrado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissão para votar"),
            @ApiResponse(responseCode = "404", description = "O votante não votou nesta eleição")
    })
    public ComprovanteResponse comprovante(@RequestParam Long eleicaoId) {
        return urnaService.consultarComprovante(eleicaoId);
    }
}
