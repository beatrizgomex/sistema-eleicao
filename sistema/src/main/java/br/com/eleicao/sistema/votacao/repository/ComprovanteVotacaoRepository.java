package br.com.eleicao.sistema.votacao.repository;

import br.com.eleicao.sistema.votacao.model.ComprovanteVotacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComprovanteVotacaoRepository extends JpaRepository<ComprovanteVotacao, Long> {

    boolean existsByEleitorIdAndEleicaoId(Long eleitorId, Long eleicaoId);

    boolean existsByEleitorId(Long eleitorId);

    Optional<ComprovanteVotacao> findByEleitorIdAndEleicaoId(Long eleitorId, Long eleicaoId);
}
