package br.com.eleicao.sistema.eleitor.repository;

import br.com.eleicao.sistema.eleitor.model.Eleitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EleitorRepository extends JpaRepository<Eleitor, Long> {

    boolean existsByDocumento(String documento);
}