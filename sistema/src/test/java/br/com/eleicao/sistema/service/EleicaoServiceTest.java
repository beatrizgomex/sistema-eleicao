
package br.com.eleicao.sistema.eleicao.service;

import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.eleicao.repository.EleicaoRepository;
import br.com.eleicao.sistema.shared.enums.SituacaoEleicao;
import br.com.eleicao.sistema.shared.enums.Turno;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EleicaoServiceTest {

    @Mock
    private EleicaoRepository repository;

    private EleicaoService service;

    private final Clock clock = Clock.fixed(
            Instant.parse("2026-11-20T12:00:00Z"),
            ZoneId.of("America/Sao_Paulo")
    );

    @BeforeEach
    void configurar() {
        service = new EleicaoService(repository, clock);
    }

    private Eleicao criarEleicao() {
        Eleicao eleicao = new Eleicao();
        eleicao.setId(1L);
        eleicao.setData(LocalDate.of(2026, 11, 20));
        eleicao.setTurno(Turno.PRIMEIRO);
        eleicao.setHoraAbertura(LocalTime.of(8, 0));
        eleicao.setHoraEncerramento(LocalTime.of(17, 0));
        eleicao.setSituacao(SituacaoEleicao.AGENDADA);
        return eleicao;
    }

    @Test
    void deveCadastrarEleicaoAgendada() {
        Eleicao eleicao = criarEleicao();

        when(repository.save(any(Eleicao.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Eleicao resultado = service.cadastrar(eleicao);

        assertEquals(SituacaoEleicao.AGENDADA,
                resultado.getSituacao());
        assertNull(resultado.getId());
        verify(repository).save(eleicao);
    }

    @Test
    void deveRejeitarHorarioInvalido() {
        Eleicao eleicao = criarEleicao();
        eleicao.setHoraEncerramento(LocalTime.of(7, 0));

        assertThrows(ResponseStatusException.class,
                () -> service.cadastrar(eleicao));

        verify(repository, never()).save(any());
    }

    @Test
    void deveAbrirEleicaoDentroDoHorario() {
        Eleicao eleicao = criarEleicao();

        when(repository.findById(1L))
                .thenReturn(Optional.of(eleicao));
        when(repository.save(any(Eleicao.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Eleicao resultado = service.abrir(1L);

        assertEquals(SituacaoEleicao.ABERTA,
                resultado.getSituacao());
        verify(repository).save(eleicao);
    }

    @Test
    void naoDeveAbrirEleicaoEncerrada() {
        Eleicao eleicao = criarEleicao();
        eleicao.setSituacao(SituacaoEleicao.ENCERRADA);

        when(repository.findById(1L))
                .thenReturn(Optional.of(eleicao));

        assertThrows(ResponseStatusException.class,
                () -> service.abrir(1L));

        verify(repository, never()).save(any());
    }

    @Test
    void deveEncerrarEleicaoAberta() {
        Eleicao eleicao = criarEleicao();
        eleicao.setSituacao(SituacaoEleicao.ABERTA);

        when(repository.findById(1L))
                .thenReturn(Optional.of(eleicao));
        when(repository.save(any(Eleicao.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Eleicao resultado = service.encerrar(1L);

        assertEquals(SituacaoEleicao.ENCERRADA,
                resultado.getSituacao());
    }

    @Test
    void naoDeveEncerrarEleicaoAgendada() {
        Eleicao eleicao = criarEleicao();

        when(repository.findById(1L))
                .thenReturn(Optional.of(eleicao));

        assertThrows(ResponseStatusException.class,
                () -> service.encerrar(1L));

        verify(repository, never()).save(any());
    }

    @Test
    void deveRetornarErroParaEleicaoInexistente() {
        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.buscarOuFalhar(99L)
        );

        assertEquals(404, erro.getStatusCode().value());
    }

    @Test
    void deveInformarSeEleicaoEstaAberta() {
        Eleicao eleicao = criarEleicao();
        eleicao.setSituacao(SituacaoEleicao.ABERTA);

        when(repository.findById(1L))
                .thenReturn(Optional.of(eleicao));

        assertTrue(service.estaAberta(1L));
    }


    @Test
    void naoDeveAbrirEleicaoAntesDoHorario() {
        Eleicao eleicao = criarEleicao();
        eleicao.setHoraAbertura(LocalTime.of(10, 0));

        when(repository.findById(1L))
                .thenReturn(Optional.of(eleicao));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.abrir(1L)
        );

        assertEquals(409, erro.getStatusCode().value());
        verify(repository, never()).save(any());
    }

    @Test
    void naoDeveAbrirEleicaoDepoisDoHorario() {
        Eleicao eleicao = criarEleicao();
        eleicao.setHoraEncerramento(LocalTime.of(8, 30));

        when(repository.findById(1L))
                .thenReturn(Optional.of(eleicao));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.abrir(1L)
        );

        assertEquals(409, erro.getStatusCode().value());
        verify(repository, never()).save(any());
    }

}
