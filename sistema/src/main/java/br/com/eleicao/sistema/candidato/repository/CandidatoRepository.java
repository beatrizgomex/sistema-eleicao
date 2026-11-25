package br.com.eleicao.sistema.candidato.repository;

import br.com.eleicao.sistema.candidato.model.Candidato;
import br.com.eleicao.sistema.shared.enums.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidatoRepository extends JpaRepository<Candidato, Long> {

    List<Candidato> findByEleicaoIdAndCargo(Long eleicaoId, Cargo cargo);

    boolean existsByEleicaoIdAndCargoAndNumero(Long eleicaoId, Cargo cargo, Integer numero);

    Optional<Candidato> findByEleicaoIdAndCargoAndNumero(Long eleicaoId, Cargo cargo, Integer numero);

    Optional<Candidato> findByUsuarioId(Long usuarioId);

    boolean existsByPartidoId(Long partidoId);

    @Query("select distinct c.cargo from Candidato c where c.eleicao.id = :eleicaoId")
    List<Cargo> findCargosEmDisputa(@Param("eleicaoId") Long eleicaoId);
}