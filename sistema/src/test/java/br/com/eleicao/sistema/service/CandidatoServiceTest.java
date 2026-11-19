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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidatoServiceTest {

    @Mock CandidatoRepository candidatoRepository;
    @Mock PartidoRepository partidoRepository;
    @Mock UsuarioService usuarioService;
    @Mock EntityManager entityManager;

    CandidatoService candidatoService;

    @BeforeEach
    void setUp() {
        candidatoService = new CandidatoService(candidatoRepository, partidoRepository, usuarioService);
        ReflectionTestUtils.setField(candidatoService, "entityManager", entityManager);
    }

    private Partido partido(Long id) {
        Partido p = new Partido();
        p.setId(id);
        p.setSigla("PAA");
        p.setNome("Partido A");
        return p;
    }

    private Eleicao eleicao(Long id) {
        Eleicao e = new Eleicao();
        e.setId(id);
        return e;
    }

    private Candidato candidatoExistente() {
        Candidato c = new Candidato();
        c.setId(1L);
        c.setNome("Joao");
        c.setNumero(10);
        c.setCargo(Cargo.GOVERNADOR);
        c.setPartido(partido(1L));
        c.setEleicao(eleicao(1L));
        return c;
    }

    private CandidatoRequest requisicao() {
        return new CandidatoRequest("Joao", 10, Cargo.GOVERNADOR, 1L, 1L, "joao", "abc");
    }

    @Test
    void cadastrar_criaUsuarioComPerfilCandidato() {
        when(candidatoRepository.existsByEleicaoIdAndCargoAndNumero(1L, Cargo.GOVERNADOR, 10))
                .thenReturn(false);
        when(partidoRepository.findById(1L)).thenReturn(Optional.of(partido(1L)));
        when(entityManager.find(Eleicao.class, 1L)).thenReturn(eleicao(1L));
        when(usuarioService.criar("joao", "abc", Perfil.CANDIDATO)).thenReturn(new Usuario());
        when(candidatoRepository.save(any(Candidato.class))).thenAnswer(i -> i.getArgument(0));

        CandidatoResponse resp = candidatoService.cadastrarCandidato(requisicao());

        assertEquals("Joao", resp.nome());
        assertEquals(10, resp.numero());
        assertEquals(Cargo.GOVERNADOR, resp.cargo());
        assertEquals("PAA", resp.partidoSigla());
        verify(usuarioService).criar("joao", "abc", Perfil.CANDIDATO);
    }

    @Test
    void cadastrar_numeroRepetidoNoCargo() {
        when(candidatoRepository.existsByEleicaoIdAndCargoAndNumero(1L, Cargo.GOVERNADOR, 10))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> candidatoService.cadastrarCandidato(requisicao()));

        assertEquals(409, ex.getStatusCode().value());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void cadastrar_partidoInexistente() {
        when(candidatoRepository.existsByEleicaoIdAndCargoAndNumero(1L, Cargo.GOVERNADOR, 10))
                .thenReturn(false);
        when(partidoRepository.findById(1L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> candidatoService.cadastrarCandidato(requisicao()));

        assertEquals(404, ex.getStatusCode().value());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void cadastrar_eleicaoInexistente() {
        when(candidatoRepository.existsByEleicaoIdAndCargoAndNumero(1L, Cargo.GOVERNADOR, 10))
                .thenReturn(false);
        when(partidoRepository.findById(1L)).thenReturn(Optional.of(partido(1L)));
        when(entityManager.find(Eleicao.class, 1L)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> candidatoService.cadastrarCandidato(requisicao()));

        assertEquals(404, ex.getStatusCode().value());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void listarDisponiveis_devolveCandidatosDoCargoNaEleicao() {
        when(candidatoRepository.findByEleicaoIdAndCargo(1L, Cargo.GOVERNADOR))
                .thenReturn(List.of(candidatoExistente()));

        List<CandidatoResponse> lista = candidatoService.listarDisponiveis(1L, Cargo.GOVERNADOR);

        assertEquals(1, lista.size());
        assertEquals("Joao", lista.get(0).nome());
    }

    @Test
    void atualizar_alteraNomeEPartido() {
        when(candidatoRepository.findById(1L)).thenReturn(Optional.of(candidatoExistente()));
        Partido novo = partido(2L);
        novo.setSigla("PBB");
        when(partidoRepository.findById(2L)).thenReturn(Optional.of(novo));
        when(candidatoRepository.save(any(Candidato.class))).thenAnswer(i -> i.getArgument(0));

        CandidatoResponse resp = candidatoService.atualizar(1L, new CandidatoUpdateRequest("Joao S.", 2L));

        assertEquals("Joao S.", resp.nome());
        assertEquals("PBB", resp.partidoSigla());
    }

    @Test
    void excluir_inexistente() {
        when(candidatoRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> candidatoService.excluir(99L));

        assertEquals(404, ex.getStatusCode().value());
    }
}