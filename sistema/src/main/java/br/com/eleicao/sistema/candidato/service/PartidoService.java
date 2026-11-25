package br.com.eleicao.sistema.candidato.service;

import br.com.eleicao.sistema.candidato.dto.PartidoRequest;
import br.com.eleicao.sistema.candidato.model.Partido;
import br.com.eleicao.sistema.candidato.repository.CandidatoRepository;
import br.com.eleicao.sistema.candidato.repository.PartidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartidoService {

    private final PartidoRepository partidoRepository;
    private final CandidatoRepository candidatoRepository;

    @Transactional
    public Partido cadastrarPartido(PartidoRequest request) {
        if (partidoRepository.existsBySigla(request.sigla())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sigla já cadastrada");
        }
        Partido partido = new Partido();
        partido.setSigla(request.sigla());
        partido.setNome(request.nome());
        return partidoRepository.save(partido);
    }

    private Partido buscarPartido(Long id) {
        return partidoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Partido não encontrado"));
    }

    public List<Partido> listarPartidos() {
        return partidoRepository.findAll();
    }

    public Partido buscarPorId(Long id) {
        return buscarPartido(id);
    }

    @Transactional
    public void excluir(Long id) {
        Partido partido = buscarPorId(id);
        if (candidatoRepository.existsByPartidoId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Partido não pode ser excluído: possui candidatos vinculados");
        }
        partidoRepository.delete(partido);
    }

    @Transactional
    public Partido atualizarPartido(Long id, PartidoRequest request) {
        Partido partido = buscarPartido(id);
        if (!partido.getSigla().equals(request.sigla()) && partidoRepository.existsBySigla(request.sigla())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sigla já cadastrada");
        }
        partido.setSigla(request.sigla());
        partido.setNome(request.nome());
        return partidoRepository.save(partido);
    }
}