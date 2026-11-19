package br.com.eleicao.sistema.eleitor.controller;

import br.com.eleicao.sistema.eleitor.dto.EleitorRequest;
import br.com.eleicao.sistema.eleitor.dto.EleitorResponse;
import br.com.eleicao.sistema.eleitor.dto.EleitorUpdateRequest;
import br.com.eleicao.sistema.eleitor.service.EleitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eleitores")
@RequiredArgsConstructor
@Tag(name = "Eleitores")
public class EleitorController {

    private final EleitorService eleitorService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um eleitor e o seu usuário de acesso")
    public EleitorResponse cadastrar(@Valid @RequestBody EleitorRequest request) {
        return eleitorService.cadastrarEleitor(request);
    }

    @GetMapping
    @Operation(summary = "Lista todos os eleitores")
    public List<EleitorResponse> listar() {
        return eleitorService.listarEleitores();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um eleitor")
    public EleitorResponse buscar(@PathVariable Long id) {
        return eleitorService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera os dados de um eleitor")
    public EleitorResponse atualizar(@PathVariable Long id,
                                     @Valid @RequestBody EleitorUpdateRequest request) {
        return eleitorService.atualizarEleitor(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um eleitor")
    public void excluir(@PathVariable Long id) {
        eleitorService.excluir(id);
    }
}