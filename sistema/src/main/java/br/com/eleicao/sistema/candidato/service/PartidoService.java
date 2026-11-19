package br.com.eleicao.sistema.candidato.service;

import br.com.eleicao.sistema.candidato.dto.PartidoRequest;
import br.com.eleicao.sistema.candidato.model.Partido;
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

    public void excluir(Long id) {
        partidoRepository.delete(buscarPorId(id));
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