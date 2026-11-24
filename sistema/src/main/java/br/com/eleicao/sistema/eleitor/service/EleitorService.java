package br.com.eleicao.sistema.eleitor.service;

import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.auth.service.UsuarioService;
import br.com.eleicao.sistema.eleitor.dto.EleitorRequest;
import br.com.eleicao.sistema.eleitor.dto.EleitorResponse;
import br.com.eleicao.sistema.eleitor.dto.EleitorUpdateRequest;
import br.com.eleicao.sistema.eleitor.model.Eleitor;
import br.com.eleicao.sistema.eleitor.repository.EleitorRepository;
import br.com.eleicao.sistema.shared.enums.Perfil;
import br.com.eleicao.sistema.votacao.repository.ComprovanteVotacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EleitorService {

    private final EleitorRepository eleitorRepository;
    private final UsuarioService usuarioService;
    private final ComprovanteVotacaoRepository comprovanteRepository;

    @Transactional
    public EleitorResponse cadastrarEleitor(EleitorRequest request) {
        if (eleitorRepository.existsByDocumento(request.documento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Documento já cadastrado");
        }
        Usuario usuario = usuarioService.criar(request.login(), request.senha(), Perfil.ELEITOR);

        Eleitor eleitor = new Eleitor();
        eleitor.setNome(request.nome());
        eleitor.setDocumento(request.documento());
        eleitor.setUsuario(usuario);
        return toResponse(eleitorRepository.save(eleitor));
    }

    public List<EleitorResponse> listarEleitores() {
        return eleitorRepository.findAll().stream().map(this::toResponse).toList();
    }

    public EleitorResponse buscarPorId(Long id) {
        return toResponse(buscar(id));
    }

    @Transactional
    public EleitorResponse atualizarEleitor(Long id, EleitorUpdateRequest request) {
        Eleitor eleitor = buscar(id);
        if (!eleitor.getDocumento().equals(request.documento())
                && eleitorRepository.existsByDocumento(request.documento())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Documento já cadastrado");
        }
        eleitor.setNome(request.nome());
        eleitor.setDocumento(request.documento());
        return toResponse(eleitorRepository.save(eleitor));
    }

    public void excluir(Long id) {
        Eleitor eleitor = buscar(id);

        if (comprovanteRepository.existsByEleitorId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Eleitor não pode ser excluído: já participou de uma votação");
        }
        eleitorRepository.delete(eleitor);
    }

    private Eleitor buscar(Long id) {
        return eleitorRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Eleitor não encontrado"));
    }

    private EleitorResponse toResponse(Eleitor eleitor) {
        return new EleitorResponse(eleitor.getId(), eleitor.getNome(), eleitor.getDocumento());
    }
}