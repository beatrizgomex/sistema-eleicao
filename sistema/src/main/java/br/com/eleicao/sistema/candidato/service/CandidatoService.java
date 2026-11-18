package br.com.eleicao.sistema.candidato.service;

import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.auth.service.UsuarioService;
import br.com.eleicao.sistema.candidato.dto.CandidatoRequest;
import br.com.eleicao.sistema.candidato.dto.CandidatoResponse;
import br.com.eleicao.sistema.candidato.dto.CandidatoUpdateRequest;
import br.com.eleicao.sistema.candidato.model.Candidato;
import br.com.eleicao.sistema.candidato.model.Partido;
import br.com.eleicao.sistema.candidato.repository.CandidatoRepository;
import br.com.eleicao.sistema.candidato.repository.PartidoRepository;
import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.shared.enums.Cargo;
import br.com.eleicao.sistema.shared.enums.Perfil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;
    private final PartidoRepository partidoRepository;
    private final UsuarioService usuarioService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public CandidatoResponse cadastrarCandidato(CandidatoRequest request) {
        if (candidatoRepository.existsByEleicaoIdAndCargoAndNumero(
                request.eleicaoId(), request.cargo(), request.numero())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe candidato com este número neste cargo e eleição");
        }
        Partido partido = buscarPartido(request.partidoId());
        Eleicao eleicao = buscarEleicao(request.eleicaoId());
        Usuario usuario = usuarioService.criar(request.login(), request.senha(), Perfil.CANDIDATO);

        Candidato candidato = new Candidato();
        candidato.setNome(request.nome());
        candidato.setNumero(request.numero());
        candidato.setCargo(request.cargo());
        candidato.setPartido(partido);
        candidato.setEleicao(eleicao);
        candidato.setUsuario(usuario);
        return toResponse(candidatoRepository.save(candidato));
    }

    public List<CandidatoResponse> listarCandidatos() {
        return candidatoRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CandidatoResponse buscarPorId(Long id) {
        return toResponse(buscar(id));
    }

    public List<CandidatoResponse> listarDisponiveis(Long eleicaoId, Cargo cargo) {
        return candidatoRepository.findByEleicaoIdAndCargo(eleicaoId, cargo).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public CandidatoResponse atualizar(Long id, CandidatoUpdateRequest req) {
        Candidato candidato = buscar(id);
        candidato.setNome(req.nome());
        candidato.setPartido(buscarPartido(req.partidoId()));
        return toResponse(candidatoRepository.save(candidato));
    }

    public void excluir(Long id) {
        candidatoRepository.delete(buscar(id));
    }

    private Candidato buscar(Long id) {
        return candidatoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidato não encontrado"));
    }

    private Partido buscarPartido(Long id) {
        return partidoRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Partido não encontrado"));
    }

    // Quando a Pessoa 2 entregar, pode trocar por EleicaoService.buscarOuFalhar(id).
    private Eleicao buscarEleicao(Long id) {
        Eleicao eleicao = entityManager.find(Eleicao.class, id);
        if (eleicao == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Eleição não encontrada");
        }
        return eleicao;
    }

    private CandidatoResponse toResponse(Candidato c) {
        return new CandidatoResponse(c.getId(), c.getNome(), c.getNumero(), c.getCargo(),
                c.getPartido().getId(), c.getPartido().getSigla(), c.getEleicao().getId());
    }
}