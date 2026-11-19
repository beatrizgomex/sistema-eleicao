package br.com.eleicao.sistema.candidato.repository;

import br.com.eleicao.sistema.candidato.model.Candidato;
import br.com.eleicao.sistema.shared.enums.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidatoRepository extends JpaRepository<Candidato, Long> {

    List<Candidato> findByEleicaoIdAndCargo(Long eleicaoId, Cargo cargo);

    boolean existsByEleicaoIdAndCargoAndNumero(Long eleicaoId, Cargo cargo, Integer numero);
}