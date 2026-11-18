
package br.com.eleicao.sistema.eleicao.repository;

import br.com.eleicao.sistema.eleicao.model.Eleicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EleicaoRepository extends JpaRepository<Eleicao, Long> {

}
