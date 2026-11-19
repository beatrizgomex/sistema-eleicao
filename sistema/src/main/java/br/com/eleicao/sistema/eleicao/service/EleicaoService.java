
package br.com.eleicao.sistema.eleicao.service;

import org.springframework.beans.factory.annotation.Autowired;
import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.eleicao.repository.EleicaoRepository;
import br.com.eleicao.sistema.shared.enums.SituacaoEleicao;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EleicaoService {

    private final EleicaoRepository repository;
    private final Clock clock;

@Autowired
public EleicaoService(EleicaoRepository repository) {
    this(repository, Clock.systemDefaultZone());
}

    // Permite controlar o horario nos testes unitarios.
    EleicaoService(EleicaoRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Eleicao cadastrar(Eleicao eleicao) {
        validarHorarios(eleicao);

        eleicao.setId(null);
        eleicao.setSituacao(SituacaoEleicao.AGENDADA);

        return repository.save(eleicao);
    }

    public List<Eleicao> listar() {
        return repository.findAll();
    }

    public Eleicao buscarOuFalhar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Eleicao nao encontrada: " + id
                ));
    }

    public boolean estaAberta(Long id) {
        return buscarOuFalhar(id).getSituacao()
                == SituacaoEleicao.ABERTA;
    }

    public Eleicao alterar(Long id, Eleicao dados) {
        Eleicao eleicao = buscarOuFalhar(id);

        if (eleicao.getSituacao() != SituacaoEleicao.AGENDADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Somente eleicoes agendadas podem ser alteradas"
            );
        }

        validarHorarios(dados);

        eleicao.setData(dados.getData());
        eleicao.setTurno(dados.getTurno());
        eleicao.setHoraAbertura(dados.getHoraAbertura());
        eleicao.setHoraEncerramento(dados.getHoraEncerramento());

        return repository.save(eleicao);
    }

    public Eleicao abrir(Long id) {
        Eleicao eleicao = buscarOuFalhar(id);

        validarTransicao(eleicao, SituacaoEleicao.ABERTA);

        LocalDateTime agora = LocalDateTime.now(clock);

        LocalDateTime abertura = LocalDateTime.of(
                eleicao.getData(), eleicao.getHoraAbertura()
        );

        LocalDateTime encerramento = LocalDateTime.of(
                eleicao.getData(), eleicao.getHoraEncerramento()
        );

        if (agora.isBefore(abertura) ||
                !agora.isBefore(encerramento)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Eleicao fora do horario permitido para abertura"
            );
        }

        eleicao.setSituacao(SituacaoEleicao.ABERTA);

        return repository.save(eleicao);
    }

    public Eleicao encerrar(Long id) {
        Eleicao eleicao = buscarOuFalhar(id);

        validarTransicao(eleicao, SituacaoEleicao.ENCERRADA);

        eleicao.setSituacao(SituacaoEleicao.ENCERRADA);

        return repository.save(eleicao);
    }

    private void validarTransicao(
            Eleicao eleicao,
            SituacaoEleicao destino) {

        if (!eleicao.getSituacao().podeIrPara(destino)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transicao de situacao nao permitida"
            );
        }
    }

    private void validarHorarios(Eleicao eleicao) {
        if (eleicao.getData() == null ||
                eleicao.getTurno() == null ||
                eleicao.getHoraAbertura() == null ||
                eleicao.getHoraEncerramento() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Data, turno e horarios sao obrigatorios"
            );
        }

        if (!eleicao.getHoraEncerramento()
                .isAfter(eleicao.getHoraAbertura())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Hora de encerramento deve ser maior que a abertura"
            );
        }
    }
}
