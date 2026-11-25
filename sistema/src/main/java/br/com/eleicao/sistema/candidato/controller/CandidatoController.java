package br.com.eleicao.sistema.candidato.controller;

import br.com.eleicao.sistema.candidato.dto.CandidatoRequest;
import br.com.eleicao.sistema.candidato.dto.CandidatoResponse;
import br.com.eleicao.sistema.candidato.dto.CandidatoUpdateRequest;
import br.com.eleicao.sistema.candidato.service.CandidatoService;
import br.com.eleicao.sistema.shared.enums.Cargo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/candidatos")
@RequiredArgsConstructor
@Tag(name = "Candidatos")
public class CandidatoController {

    private final CandidatoService candidatoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um candidato e o seu usuário de acesso")
    public CandidatoResponse cadastrar(@Valid @RequestBody CandidatoRequest request) {
        return candidatoService.cadastrarCandidato(request);
    }

    @GetMapping
    @Operation(summary = "Lista candidatos. Com eleicaoId e cargo, lista os disponíveis para votação")
    public List<CandidatoResponse> listar(@RequestParam(required = false) Long eleicaoId,
                                          @RequestParam(required = false) Cargo cargo) {
        if (eleicaoId != null && cargo != null) {
            return candidatoService.listarDisponiveis(eleicaoId, cargo);
        }
        return candidatoService.listarCandidatos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um candidato")
    public CandidatoResponse buscar(@PathVariable Long id) {
        return candidatoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera nome e partido de um candidato")
    public CandidatoResponse atualizar(@PathVariable Long id,
                                       @Valid @RequestBody CandidatoUpdateRequest request) {
        return candidatoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um candidato")
    public void excluir(@PathVariable Long id) {
        candidatoService.excluir(id);
    }
}