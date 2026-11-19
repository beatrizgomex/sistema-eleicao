
package br.com.eleicao.sistema.eleicao.controller;

import br.com.eleicao.sistema.eleicao.dto.EleicaoRequestDTO;
import br.com.eleicao.sistema.eleicao.dto.EleicaoResponseDTO;
import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.eleicao.service.EleicaoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eleicoes")
@Tag(name = "Eleições", description = "Cadastro e controle do ciclo de vida das eleições")
public class EleicaoController {

    private final EleicaoService service;

    public EleicaoController(EleicaoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Cadastrar eleição",
            description = "Cadastra uma eleição com situação inicial AGENDADA.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Eleição cadastrada"),
            @ApiResponse(responseCode = "400", description = "Dados ou horários inválidos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado"),
            @ApiResponse(responseCode = "403", description = "Acesso não autorizado")
    })
    public ResponseEntity<EleicaoResponseDTO> cadastrar(
            @Valid @RequestBody EleicaoRequestDTO request) {

        Eleicao eleicao = converterParaEntidade(request);
        Eleicao salva = service.cadastrar(eleicao);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EleicaoResponseDTO.fromEntity(salva));
    }

    @GetMapping
    @Operation(summary = "Listar eleições",
            description = "Lista todas as eleições e suas situações.")
    @ApiResponse(responseCode = "200", description = "Eleições consultadas")
    public List<EleicaoResponseDTO> listar() {
        return service.listar().stream()
                .map(EleicaoResponseDTO::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar eleição",
            description = "Consulta uma eleição pelo ID e informa sua situação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eleição encontrada"),
            @ApiResponse(responseCode = "404", description = "Eleição não encontrada")
    })
    public EleicaoResponseDTO buscar(@PathVariable Long id) {
        return EleicaoResponseDTO.fromEntity(
                service.buscarOuFalhar(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Alterar eleição",
            description = "Altera os dados de uma eleição AGENDADA.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eleição alterada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Eleição não encontrada"),
            @ApiResponse(responseCode = "409", description = "Eleição não pode ser alterada")
    })
    public EleicaoResponseDTO alterar(
            @PathVariable Long id,
            @Valid @RequestBody EleicaoRequestDTO request) {

        Eleicao dados = converterParaEntidade(request);

        return EleicaoResponseDTO.fromEntity(
                service.alterar(id, dados));
    }

    @PatchMapping("/{id}/abrir")
    @Operation(summary = "Abrir eleição",
            description = "Abre manualmente uma eleição AGENDADA, somente dentro da janela de horário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eleição aberta"),
            @ApiResponse(responseCode = "404", description = "Eleição não encontrada"),
            @ApiResponse(responseCode = "409", description = "Situação ou horário não permite abertura")
    })
    public EleicaoResponseDTO abrir(@PathVariable Long id) {
        return EleicaoResponseDTO.fromEntity(
                service.abrir(id));
    }

    @PatchMapping("/{id}/encerrar")
    @Operation(summary = "Encerrar eleição",
            description = "Encerra manualmente uma eleição ABERTA. Uma eleição encerrada não pode ser reaberta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Eleição encerrada"),
            @ApiResponse(responseCode = "404", description = "Eleição não encontrada"),
            @ApiResponse(responseCode = "409", description = "Eleição não está aberta")
    })
    public EleicaoResponseDTO encerrar(@PathVariable Long id) {
        return EleicaoResponseDTO.fromEntity(
                service.encerrar(id));
    }

    private Eleicao converterParaEntidade(EleicaoRequestDTO request) {
        Eleicao eleicao = new Eleicao();

        eleicao.setData(request.data());
        eleicao.setTurno(request.turno());
        eleicao.setHoraAbertura(request.horaAbertura());
        eleicao.setHoraEncerramento(request.horaEncerramento());

        return eleicao;
    }
}
