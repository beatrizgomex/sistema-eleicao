package br.com.eleicao.sistema.votacao.repository;

import br.com.eleicao.sistema.votacao.model.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface VotoRepository extends JpaRepository<Voto, UUID> {
}
