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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EleitorServiceTest {

    @Mock EleitorRepository eleitorRepository;
    @Mock ComprovanteVotacaoRepository comprovanteRepository;
    @Mock UsuarioService usuarioService;
    @InjectMocks EleitorService eleitorService;

    private Eleitor eleitorExistente() {
        Eleitor e = new Eleitor();
        e.setId(1L);
        e.setNome("Maria");
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        e.setUsuario(usuario);
        e.setDocumento("111");
        return e;
    }

    @Test
    void cadastrar_criaUsuarioComPerfilEleitor() {
        when(eleitorRepository.existsByDocumento("111")).thenReturn(false);
        when(usuarioService.criar("maria", "abc", Perfil.ELEITOR)).thenReturn(new Usuario());
        when(eleitorRepository.save(any(Eleitor.class))).thenAnswer(i -> i.getArgument(0));

        EleitorResponse response = eleitorService.cadastrarEleitor(
                new EleitorRequest("Maria", "111", "maria", "abc"));

        assertEquals("Maria", response.nome());
        assertEquals("111", response.documento());
        verify(usuarioService).criar("maria", "abc", Perfil.ELEITOR);
    }

    @Test
    void cadastrar_documentoRepetido() {
        when(eleitorRepository.existsByDocumento("111")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eleitorService.cadastrarEleitor(new EleitorRequest("Maria", "111", "maria", "abc")));

        assertEquals(409, ex.getStatusCode().value());
        verifyNoInteractions(usuarioService);
    }

    @Test
    void buscarPorId_inexistente() {
        when(eleitorRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eleitorService.buscarPorId(99L));

        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void atualizar_documento() {
        when(eleitorRepository.findById(1L)).thenReturn(Optional.of(eleitorExistente()));
        when(eleitorRepository.existsByDocumento("222")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eleitorService.atualizarEleitor(1L, new EleitorUpdateRequest("Maria", "222")));

        assertEquals(409, ex.getStatusCode().value());
        verify(eleitorRepository, never()).save(any());
    }

    @Test
    void atualizar_mantendoOMesmoDocumento() {
        when(eleitorRepository.findById(1L)).thenReturn(Optional.of(eleitorExistente()));
        when(eleitorRepository.save(any(Eleitor.class))).thenAnswer(i -> i.getArgument(0));
        EleitorResponse response = eleitorService.atualizarEleitor(1L, new EleitorUpdateRequest("Maria B", "111"));
        assertEquals("Maria B", response.nome());
        verify(eleitorRepository, never()).existsByDocumento(anyString());
    }

    @Test
    void excluir_removeOEleitor() {
        Eleitor eleitor = eleitorExistente();
        when(eleitorRepository.findById(1L)).thenReturn(Optional.of(eleitor));
        eleitorService.excluir(1L);
        verify(eleitorRepository).delete(eleitor);
    }
}