package br.com.eleicao.sistema.votacao.repository;

import br.com.eleicao.sistema.votacao.model.ComprovanteVotacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComprovanteVotacaoRepository extends JpaRepository<ComprovanteVotacao, Long> {

    boolean existsByUsuarioIdAndEleicaoId(Long usuarioId, Long eleicaoId);

    boolean existsByUsuarioId(Long usuarioId);

    Optional<ComprovanteVotacao> findByUsuarioIdAndEleicaoId(Long usuarioId, Long eleicaoId);
}
