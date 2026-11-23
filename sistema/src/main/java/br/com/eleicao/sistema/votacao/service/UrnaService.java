package br.com.eleicao.sistema.votacao.service;

import br.com.eleicao.sistema.auth.service.UsuarioLogado;
import br.com.eleicao.sistema.candidato.repository.CandidatoRepository;
import br.com.eleicao.sistema.candidato.service.CandidatoService;
import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.eleicao.service.EleicaoService;
import br.com.eleicao.sistema.eleitor.model.Eleitor;
import br.com.eleicao.sistema.eleitor.repository.EleitorRepository;
import br.com.eleicao.sistema.shared.enums.Cargo;
import br.com.eleicao.sistema.shared.enums.Perfil;
import br.com.eleicao.sistema.shared.enums.SituacaoEleicao;
import br.com.eleicao.sistema.shared.enums.TipoVoto;
import br.com.eleicao.sistema.votacao.dto.CargoNaCedulaResponse;
import br.com.eleicao.sistema.votacao.dto.CedulaRequest;
import br.com.eleicao.sistema.votacao.dto.ComprovanteResponse;
import br.com.eleicao.sistema.votacao.dto.VotoRequest;
import br.com.eleicao.sistema.votacao.model.ComprovanteVotacao;
import br.com.eleicao.sistema.votacao.model.Voto;
import br.com.eleicao.sistema.votacao.repository.ComprovanteVotacaoRepository;
import br.com.eleicao.sistema.votacao.repository.VotoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UrnaService {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final EleitorRepository eleitorRepository;
    private final CandidatoRepository candidatoRepository;
    private final CandidatoService candidatoService;
    private final EleicaoService eleicaoService;
    private final VotoRepository votoRepository;
    private final ComprovanteVotacaoRepository comprovanteRepository;
    private final ObjectProvider<UsuarioLogado> usuarioLogado;

    @PersistenceContext
    private EntityManager entityManager;

    public List<CargoNaCedulaResponse> montarCedula(Long eleicaoId) {
        eleicaoService.buscarOuFalhar(eleicaoId);
        return cargosEmDisputa(eleicaoId).stream()
                .map(cargo -> new CargoNaCedulaResponse(cargo, cargo.getDescricao(),
                        candidatoService.listarDisponiveis(eleicaoId, cargo)))
                .toList();
    }

    @Transactional
    public ComprovanteResponse votar(CedulaRequest cedula) {
        Eleitor eleitor = eleitorLogado();

        Eleicao eleicao = entityManager.find(Eleicao.class, cedula.eleicaoId(), LockModeType.PESSIMISTIC_READ);
        if (eleicao == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Eleição não encontrada");
        }
        if (eleicao.getSituacao() != SituacaoEleicao.ABERTA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A eleição não está aberta para votação");
        }
        if (comprovanteRepository.existsByEleitorIdAndEleicaoId(eleitor.getId(), eleicao.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Eleitor já votou nesta eleição");
        }

        List<Voto> votos = montarVotos(eleicao, cedula.votos());

        ComprovanteVotacao comprovante = new ComprovanteVotacao();
        comprovante.setEleitor(eleitor);
        comprovante.setEleicao(eleicao);
        comprovante.setCodigo(gerarCodigo());
        comprovante.setEmitidoEm(LocalDateTime.now());
        try {
            comprovanteRepository.saveAndFlush(comprovante);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Eleitor já votou nesta eleição");
        }
        votoRepository.saveAll(votos);
        return toResponse(comprovante);
    }

    public ComprovanteResponse consultarComprovante(Long eleicaoId) {
        Eleitor eleitor = eleitorLogado();
        return comprovanteRepository.findByEleitorIdAndEleicaoId(eleitor.getId(), eleicaoId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhum comprovante deste eleitor nesta eleição"));
    }

    private Eleitor eleitorLogado() {
        UsuarioLogado usuario = usuarioLogado.getIfAvailable();
        if (usuario == null || usuario.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        if (usuario.getPerfil() != Perfil.ELEITOR) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente eleitores podem votar");
        }
        return eleitorRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Eleitor não encontrado"));
    }

    private List<Cargo> cargosEmDisputa(Long eleicaoId) {
        return candidatoRepository.findCargosEmDisputa(eleicaoId).stream().sorted().toList();
    }

    private List<Voto> montarVotos(Eleicao eleicao, List<VotoRequest> pedidos) {
        Set<Cargo> faltando = EnumSet.noneOf(Cargo.class);
        faltando.addAll(cargosEmDisputa(eleicao.getId()));
        Set<Cargo> votados = EnumSet.noneOf(Cargo.class);

        List<Voto> votos = new ArrayList<>();
        for (VotoRequest pedido : pedidos) {
            if (!votados.add(pedido.cargo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O cargo " + pedido.cargo().getDescricao() + " aparece mais de uma vez na cédula");
            }
            if (!faltando.remove(pedido.cargo())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Não há disputa para " + pedido.cargo().getDescricao() + " nesta eleição");
            }
            votos.add(montarVoto(eleicao, pedido));
        }
        if (!faltando.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta o voto para: "
                    + faltando.stream().map(Cargo::getDescricao).collect(Collectors.joining(", ")));
        }
        return votos;
    }

    private Voto montarVoto(Eleicao eleicao, VotoRequest pedido) {
        Voto voto = new Voto();
        voto.setEleicao(eleicao);
        voto.setCargo(pedido.cargo());
        voto.setTipo(pedido.tipo());

        if (pedido.tipo() == TipoVoto.CANDIDATO) {
            if (pedido.numero() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voto em candidato precisa do número");
            }
            voto.setCandidato(candidatoRepository
                    .findByEleicaoIdAndCargoAndNumero(eleicao.getId(), pedido.cargo(), pedido.numero())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Nenhum candidato com o número " + pedido.numero()
                                    + " para " + pedido.cargo().getDescricao())));
        } else if (pedido.numero() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Voto em branco ou nulo não leva número");
        }
        return voto;
    }

    private String gerarCodigo() {
        byte[] bytes = new byte[8];
        ALEATORIO.nextBytes(bytes);
        String hex = HexFormat.of().withUpperCase().formatHex(bytes);
        return String.join("-", hex.substring(0, 4), hex.substring(4, 8), hex.substring(8, 12), hex.substring(12));
    }

    private ComprovanteResponse toResponse(ComprovanteVotacao comprovante) {
        return new ComprovanteResponse(comprovante.getCodigo(), comprovante.getEleicao().getId(),
                comprovante.getEmitidoEm());
    }
}
