package br.com.eleicao.sistema.candidato.repository;

import br.com.eleicao.sistema.candidato.model.Partido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PartidoRepository extends JpaRepository<Partido, Long> {

    boolean existsBySigla(String sigla);
}