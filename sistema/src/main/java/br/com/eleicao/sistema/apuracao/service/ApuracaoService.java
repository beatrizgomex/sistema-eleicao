package br.com.eleicao.sistema.apuracao.service;

import br.com.eleicao.sistema.apuracao.dto.ApuracaoParcialResponse;
import br.com.eleicao.sistema.apuracao.dto.CandidatoVotosResponse;
import br.com.eleicao.sistema.apuracao.dto.ResultadoCargoResponse;
import br.com.eleicao.sistema.auth.service.UsuarioLogado;
import br.com.eleicao.sistema.candidato.repository.CandidatoRepository;
import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.eleicao.service.EleicaoService;
import br.com.eleicao.sistema.shared.enums.Cargo;
import br.com.eleicao.sistema.shared.enums.Perfil;
import br.com.eleicao.sistema.shared.enums.SituacaoEleicao;
import br.com.eleicao.sistema.shared.enums.TipoVoto;
import br.com.eleicao.sistema.votacao.repository.VotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApuracaoService {

    private final EleicaoService eleicaoService;
    private final CandidatoRepository candidatoRepository;
    private final VotoRepository votoRepository;
    private final ObjectProvider<UsuarioLogado> usuarioLogado;

    public ApuracaoParcialResponse parcial(Long eleicaoId, Cargo cargo) {
        UsuarioLogado usuario = usuarioAutenticado();
        Eleicao eleicao = eleicaoService.buscarOuFalhar(eleicaoId);

        if (eleicao.getSituacao() != SituacaoEleicao.ABERTA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A apuração parcial só fica disponível com a eleição ABERTA");
        }

        if (usuario.getPerfil() == Perfil.CANDIDATO) {
            verificarCargoDoCandidato(usuario.getId(), eleicaoId, cargo);
        }

        return new ApuracaoParcialResponse(
                cargo,
                candidatosComVotos(eleicaoId, cargo),
                votoRepository.countByEleicaoIdAndCargoAndTipo(eleicaoId, cargo, TipoVoto.BRANCO),
                votoRepository.countByEleicaoIdAndCargoAndTipo(eleicaoId, cargo, TipoVoto.NULO));
    }

    public List<ResultadoCargoResponse> resultado(Long eleicaoId) {
        usuarioAutenticado();
        Eleicao eleicao = eleicaoService.buscarOuFalhar(eleicaoId);

        if (eleicao.getSituacao() != SituacaoEleicao.ENCERRADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O resultado só fica disponível após o encerramento da eleição");
        }

        return candidatoRepository.findCargosEmDisputa(eleicaoId).stream()
                .sorted()
                .map(cargo -> resultadoDoCargo(eleicaoId, cargo))
                .toList();
    }

    private ResultadoCargoResponse resultadoDoCargo(Long eleicaoId, Cargo cargo) {
        List<CandidatoVotosResponse> candidatos = candidatosComVotos(eleicaoId, cargo);

        long maiorTotal = candidatos.stream()
                .mapToLong(CandidatoVotosResponse::votos)
                .max()
                .orElse(0);

        List<CandidatoVotosResponse> maisVotados = candidatos.stream()
                .filter(c -> c.votos() == maiorTotal)
                .toList();

        boolean empate = maisVotados.size() > 1;
        CandidatoVotosResponse vencedor = maisVotados.size() == 1 ? maisVotados.get(0) : null;

        return new ResultadoCargoResponse(
                cargo,
                candidatos,
                votoRepository.countByEleicaoIdAndCargoAndTipo(eleicaoId, cargo, TipoVoto.BRANCO),
                votoRepository.countByEleicaoIdAndCargoAndTipo(eleicaoId, cargo, TipoVoto.NULO),
                vencedor,
                empate);
    }

    private List<CandidatoVotosResponse> candidatosComVotos(Long eleicaoId, Cargo cargo) {
        return votoRepository.contarVotosPorCandidato(eleicaoId, cargo, TipoVoto.CANDIDATO).stream()
                .map(c -> new CandidatoVotosResponse(c.getId(), c.getNome(), c.getVotos()))
                .toList();
    }

    private void verificarCargoDoCandidato(Long usuarioId, Long eleicaoId, Cargo cargo) {
        candidatoRepository.findByUsuarioId(usuarioId)
                .filter(c -> c.getEleicao().getId().equals(eleicaoId) && c.getCargo() == cargo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Candidato só pode consultar a apuração do próprio cargo nesta eleição"));
    }

    private UsuarioLogado usuarioAutenticado() {
        UsuarioLogado usuario = usuarioLogado.getIfAvailable();
        if (usuario == null || usuario.getId() == null || usuario.getPerfil() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }
        return usuario;
    }
}
