package br.com.eleicao.sistema.service;

import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.auth.service.UsuarioLogado;
import br.com.eleicao.sistema.candidato.model.Candidato;
import br.com.eleicao.sistema.candidato.repository.CandidatoRepository;
import br.com.eleicao.sistema.candidato.service.CandidatoService;
import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.eleicao.service.EleicaoService;
import br.com.eleicao.sistema.eleitor.model.Eleitor;
import br.com.eleicao.sistema.auth.repository.UsuarioRepository;
import br.com.eleicao.sistema.shared.enums.Cargo;
import br.com.eleicao.sistema.shared.enums.Perfil;
import br.com.eleicao.sistema.shared.enums.SituacaoEleicao;
import br.com.eleicao.sistema.shared.enums.TipoVoto;
import br.com.eleicao.sistema.votacao.dto.CedulaRequest;
import br.com.eleicao.sistema.votacao.dto.ComprovanteResponse;
import br.com.eleicao.sistema.votacao.dto.VotoRequest;
import br.com.eleicao.sistema.votacao.model.ComprovanteVotacao;
import br.com.eleicao.sistema.votacao.model.Voto;
import br.com.eleicao.sistema.votacao.repository.ComprovanteVotacaoRepository;
import br.com.eleicao.sistema.votacao.repository.VotoRepository;
import br.com.eleicao.sistema.votacao.service.UrnaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Optional;

import static br.com.eleicao.sistema.shared.enums.Cargo.GOVERNADOR;
import static br.com.eleicao.sistema.shared.enums.Cargo.PRESIDENTE;
import static br.com.eleicao.sistema.shared.enums.Cargo.SENADOR;
import static br.com.eleicao.sistema.shared.enums.TipoVoto.BRANCO;
import static br.com.eleicao.sistema.shared.enums.TipoVoto.CANDIDATO;
import static br.com.eleicao.sistema.shared.enums.TipoVoto.NULO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrnaServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock CandidatoRepository candidatoRepository;
    @Mock CandidatoService candidatoService;
    @Mock EleicaoService eleicaoService;
    @Mock VotoRepository votoRepository;
    @Mock ComprovanteVotacaoRepository comprovanteRepository;
    @Mock ObjectProvider<UsuarioLogado> usuarioLogadoProvider;
    @Mock UsuarioLogado usuarioLogado;
    @Mock EntityManager entityManager;

    @Captor ArgumentCaptor<List<Voto>> votosGravados;
    @Captor ArgumentCaptor<ComprovanteVotacao> comprovanteGravado;

    UrnaService urnaService;

    @BeforeEach
    void setUp() {
        urnaService = new UrnaService(usuarioRepository, candidatoRepository, candidatoService,
                eleicaoService, votoRepository, comprovanteRepository, usuarioLogadoProvider);
        ReflectionTestUtils.setField(urnaService, "entityManager", entityManager);
    }

    private void logarComo(Perfil perfil) {
        when(usuarioLogadoProvider.getIfAvailable()).thenReturn(usuarioLogado);
        when(usuarioLogado.getId()).thenReturn(7L);
        when(usuarioLogado.getPerfil()).thenReturn(perfil);
    }

    private void logarEleitor() {
        logarComo(Perfil.ELEITOR);
        Usuario eleitor = new Usuario();
        eleitor.setId(7L);
        eleitor.setPerfil(Perfil.ELEITOR);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(eleitor));
    }

    private void eleicao(SituacaoEleicao situacao) {
        Eleicao eleicao = new Eleicao();
        eleicao.setId(1L);
        eleicao.setSituacao(situacao);
        when(entityManager.find(Eleicao.class, 1L, LockModeType.PESSIMISTIC_READ)).thenReturn(eleicao);
    }

    private void prontoParaVotar(Cargo... cargos) {
        logarEleitor();
        eleicao(SituacaoEleicao.ABERTA);
        when(comprovanteRepository.existsByUsuarioIdAndEleicaoId(7L, 1L)).thenReturn(false);
        when(candidatoRepository.findCargosEmDisputa(1L)).thenReturn(List.of(cargos));
    }

    private Candidato candidato(Cargo cargo, int numero) {
        Candidato candidato = new Candidato();
        candidato.setCargo(cargo);
        candidato.setNumero(numero);
        when(candidatoRepository.findByEleicaoIdAndCargoAndNumero(1L, cargo, numero))
                .thenReturn(Optional.of(candidato));
        return candidato;
    }

    private CedulaRequest cedula(VotoRequest... votos) {
        return new CedulaRequest(1L, List.of(votos));
    }

    private void assertStatus(HttpStatus esperado, Executable acao) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, acao);
        assertEquals(esperado, e.getStatusCode());
    }

    @Test
    @DisplayName("grava um voto por cargo e o comprovante, e devolve o código")
    void votar_cedulaCompleta() {
        prontoParaVotar(GOVERNADOR, PRESIDENTE);
        Candidato governador = candidato(GOVERNADOR, 10);
        when(comprovanteRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        ComprovanteResponse resposta = urnaService.votar(cedula(
                new VotoRequest(GOVERNADOR, CANDIDATO, 10),
                new VotoRequest(PRESIDENTE, BRANCO, null)));

        verify(votoRepository).saveAll(votosGravados.capture());
        List<Voto> votos = votosGravados.getValue();
        assertEquals(2, votos.size());
        assertEquals(governador, votos.get(0).getCandidato());
        assertEquals(BRANCO, votos.get(1).getTipo());
        assertNull(votos.get(1).getCandidato());

        verify(comprovanteRepository).saveAndFlush(comprovanteGravado.capture());
        assertEquals(7L, comprovanteGravado.getValue().getUsuario().getId());
        assertEquals(comprovanteGravado.getValue().getCodigo(), resposta.codigo());
        assertTrue(resposta.codigo().matches("[0-9A-F]{4}(-[0-9A-F]{4}){3}"));
    }

    @Test
    @DisplayName("aceita voto nulo")
    void votar_nulo() {
        prontoParaVotar(GOVERNADOR);
        when(comprovanteRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        urnaService.votar(cedula(new VotoRequest(GOVERNADOR, NULO, null)));

        verify(votoRepository).saveAll(votosGravados.capture());
        assertEquals(NULO, votosGravados.getValue().get(0).getTipo());
    }

    @Test
    @DisplayName("o voto não guarda quem votou nem quando")
    void voto_semEleitorESemHorario() {
        for (Field campo : Voto.class.getDeclaredFields()) {
            assertFalse(Eleitor.class.isAssignableFrom(campo.getType()), campo.getName());
            assertFalse(Usuario.class.isAssignableFrom(campo.getType()), campo.getName());
            assertFalse(Temporal.class.isAssignableFrom(campo.getType()), campo.getName());
        }
    }

    @Test
    @DisplayName("rejeita com 401 quando ninguém está logado")
    void votar_semUsuario() {
        when(usuarioLogadoProvider.getIfAvailable()).thenReturn(null);

        assertStatus(HttpStatus.UNAUTHORIZED, () -> urnaService.votar(cedula(new VotoRequest(GOVERNADOR, BRANCO, null))));
        verifyNoInteractions(votoRepository, comprovanteRepository);
    }

    @Test
    @DisplayName("rejeita com 403 quando o usuário não é eleitor")
    void votar_perfilChefe() {
        logarComo(Perfil.CHEFE);

        assertStatus(HttpStatus.FORBIDDEN, () -> urnaService.votar(cedula(new VotoRequest(GOVERNADOR, BRANCO, null))));
        verifyNoInteractions(votoRepository, comprovanteRepository);
    }

    @Test
    @DisplayName("rejeita com 409 quando a eleição não está aberta")
    void votar_eleicaoAgendada() {
        logarEleitor();
        eleicao(SituacaoEleicao.AGENDADA);

        assertStatus(HttpStatus.CONFLICT, () -> urnaService.votar(cedula(new VotoRequest(GOVERNADOR, BRANCO, null))));
        verifyNoInteractions(votoRepository, comprovanteRepository);
    }

    @Test
    @DisplayName("rejeita com 409 o segundo voto do mesmo eleitor")
    void votar_jaVotou() {
        logarEleitor();
        eleicao(SituacaoEleicao.ABERTA);
        when(comprovanteRepository.existsByUsuarioIdAndEleicaoId(7L, 1L)).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, () -> urnaService.votar(cedula(new VotoRequest(GOVERNADOR, BRANCO, null))));
        verifyNoInteractions(votoRepository);
    }

    @Test
    @DisplayName("rejeita com 409 dois envios simultâneos, sem gravar votos")
    void votar_envioSimultaneo() {
        prontoParaVotar(GOVERNADOR);
        when(comprovanteRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicado"));

        assertStatus(HttpStatus.CONFLICT, () -> urnaService.votar(cedula(new VotoRequest(GOVERNADOR, BRANCO, null))));
        verify(votoRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("rejeita com 400 a cédula sem voto para um cargo em disputa")
    void votar_faltaCargo() {
        prontoParaVotar(GOVERNADOR, PRESIDENTE);

        assertStatus(HttpStatus.BAD_REQUEST, () -> urnaService.votar(cedula(new VotoRequest(GOVERNADOR, BRANCO, null))));
        verifyNoInteractions(votoRepository);
    }

    @Test
    @DisplayName("rejeita com 400 o mesmo cargo votado duas vezes")
    void votar_cargoRepetido() {
        prontoParaVotar(GOVERNADOR);

        assertStatus(HttpStatus.BAD_REQUEST, () -> urnaService.votar(cedula(
                new VotoRequest(GOVERNADOR, BRANCO, null),
                new VotoRequest(GOVERNADOR, NULO, null))));
    }

    @Test
    @DisplayName("rejeita com 400 voto para cargo sem disputa na eleição")
    void votar_cargoSemDisputa() {
        prontoParaVotar(GOVERNADOR);

        assertStatus(HttpStatus.BAD_REQUEST, () -> urnaService.votar(cedula(
                new VotoRequest(GOVERNADOR, BRANCO, null),
                new VotoRequest(SENADOR, BRANCO, null))));
    }

    @Test
    @DisplayName("rejeita com 400 voto em branco com número")
    void votar_brancoComNumero() {
        prontoParaVotar(GOVERNADOR);

        assertStatus(HttpStatus.BAD_REQUEST, () -> urnaService.votar(cedula(new VotoRequest(GOVERNADOR, BRANCO, 10))));
    }

    @Test
    @DisplayName("rejeita com 400 voto em candidato sem número")
    void votar_candidatoSemNumero() {
        prontoParaVotar(GOVERNADOR);

        assertStatus(HttpStatus.BAD_REQUEST, () -> urnaService.votar(cedula(new VotoRequest(GOVERNADOR, CANDIDATO, null))));
    }

    @Test
    @DisplayName("rejeita com 404 número sem candidato no cargo")
    void votar_candidatoInexistente() {
        prontoParaVotar(GOVERNADOR);
        when(candidatoRepository.findByEleicaoIdAndCargoAndNumero(1L, GOVERNADOR, 99)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> urnaService.votar(cedula(new VotoRequest(GOVERNADOR, CANDIDATO, 99))));
        verifyNoInteractions(votoRepository);
    }

    @Test
    @DisplayName("retorna 404 no comprovante de quem não votou")
    void consultarComprovante_naoVotou() {
        logarEleitor();
        when(comprovanteRepository.findByUsuarioIdAndEleicaoId(7L, 1L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, () -> urnaService.consultarComprovante(1L));
    }

    @Test
    @DisplayName("permite que perfil CANDIDATO vote na eleição e receba comprovante")
    void votar_comoCandidato() {
        logarComo(Perfil.CANDIDATO);
        Usuario usuarioCandidato = new Usuario();
        usuarioCandidato.setId(7L);
        usuarioCandidato.setPerfil(Perfil.CANDIDATO);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuarioCandidato));

        eleicao(SituacaoEleicao.ABERTA);
        when(comprovanteRepository.existsByUsuarioIdAndEleicaoId(7L, 1L)).thenReturn(false);
        when(candidatoRepository.findCargosEmDisputa(1L)).thenReturn(List.of(GOVERNADOR));
        Candidato proprioCandidato = candidato(GOVERNADOR, 10);
        when(comprovanteRepository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));

        ComprovanteResponse resposta = urnaService.votar(cedula(new VotoRequest(GOVERNADOR, CANDIDATO, 10)));

        verify(votoRepository).saveAll(votosGravados.capture());
        assertEquals(1, votosGravados.getValue().size());
        assertEquals(proprioCandidato, votosGravados.getValue().get(0).getCandidato());

        verify(comprovanteRepository).saveAndFlush(comprovanteGravado.capture());
        assertEquals(usuarioCandidato, comprovanteGravado.getValue().getUsuario());
        assertEquals(comprovanteGravado.getValue().getCodigo(), resposta.codigo());
    }
}
