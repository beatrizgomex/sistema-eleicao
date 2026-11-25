package br.com.eleicao.sistema.candidato.service;

import br.com.eleicao.sistema.candidato.dto.PartidoRequest;
import br.com.eleicao.sistema.candidato.model.Partido;
import br.com.eleicao.sistema.candidato.repository.CandidatoRepository;
import br.com.eleicao.sistema.candidato.repository.PartidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartidoServiceTest {

    @Mock PartidoRepository partidoRepository;
    @Mock CandidatoRepository candidatoRepository;
    @InjectMocks PartidoService partidoService;

    private Partido partidoExistente() {
        Partido p = new Partido();
        p.setId(1L);
        p.setSigla("PAA");
        p.setNome("Partido A");
        return p;
    }

    @Test
    void cadastrar_gravaSiglaENome() {
        when(partidoRepository.existsBySigla("PAA")).thenReturn(false);
        when(partidoRepository.save(any(Partido.class))).thenAnswer(i -> i.getArgument(0));

        partidoService.cadastrarPartido(new PartidoRequest("PAA", "Partido A"));

        ArgumentCaptor<Partido> captor = ArgumentCaptor.forClass(Partido.class);
        verify(partidoRepository).save(captor.capture());
        assertEquals("PAA", captor.getValue().getSigla());
        assertEquals("Partido A", captor.getValue().getNome());
    }

    @Test
    void cadastrar_siglaRepetida() {
        when(partidoRepository.existsBySigla("PAA")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> partidoService.cadastrarPartido(new PartidoRequest("PAA", "Partido A")));

        assertEquals(409, ex.getStatusCode().value());
        verify(partidoRepository, never()).save(any());
    }

    @Test
    void atualizar_siglaDeOutroPartido() {
        when(partidoRepository.findById(1L)).thenReturn(Optional.of(partidoExistente()));
        when(partidoRepository.existsBySigla("PBB")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> partidoService.atualizarPartido(1L, new PartidoRequest("PBB", "Partido B")));

        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void buscarPorId_inexistente() {
        when(partidoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> partidoService.buscarPorId(99L));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void excluir_removeOPartido() {
        Partido partido = partidoExistente();
        when(partidoRepository.findById(1L)).thenReturn(Optional.of(partido));
        when(candidatoRepository.existsByPartidoId(1L)).thenReturn(false);

        partidoService.excluir(1L);

        verify(partidoRepository).delete(partido);
    }

    @Test
    void excluir_comCandidatosVinculados_retorna409() {
        Partido partido = partidoExistente();
        when(partidoRepository.findById(1L)).thenReturn(Optional.of(partido));
        when(candidatoRepository.existsByPartidoId(1L)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> partidoService.excluir(1L));

        assertEquals(409, ex.getStatusCode().value());
        verify(partidoRepository, never()).delete(any());
    }
}