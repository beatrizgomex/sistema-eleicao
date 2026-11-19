package br.com.eleicao.sistema.candidato.controller;
import br.com.eleicao.sistema.candidato.dto.PartidoRequest;
import br.com.eleicao.sistema.candidato.model.Partido;
import br.com.eleicao.sistema.candidato.service.PartidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/partidos")
@RequiredArgsConstructor
@Tag(name = "Partidos")
public class PartidoController {

    private final PartidoService partidoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um partido")
    public Partido cadastrar(@Valid @RequestBody PartidoRequest request) {
        return partidoService.cadastrarPartido(request);
    }

    @GetMapping
    @Operation(summary = "Lista os partidos")
    public List<Partido> listar() {
        return partidoService.listarPartidos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um partido")
    public Partido buscar(@PathVariable Long id) {
        return partidoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Altera um partido")
    public Partido atualizar(@PathVariable Long id,
                                     @Valid @RequestBody PartidoRequest request) {
        return partidoService.atualizarPartido(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um partido")
    public void excluir(@PathVariable Long id) {
        partidoService.excluir(id);
    }
}