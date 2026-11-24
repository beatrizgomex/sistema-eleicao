package br.com.eleicao.sistema.votacao.repository;

import br.com.eleicao.sistema.shared.enums.Cargo;
import br.com.eleicao.sistema.shared.enums.TipoVoto;
import br.com.eleicao.sistema.votacao.model.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VotoRepository extends JpaRepository<Voto, UUID> {

    interface CandidatoComVotos {
        Long getId();
        String getNome();
        Long getVotos();
    }

    long countByEleicaoIdAndCargoAndTipo(Long eleicaoId, Cargo cargo, TipoVoto tipo);

    @Query("""
            select c.id as id, c.nome as nome, count(v.id) as votos
            from Candidato c
            left join Voto v on v.candidato = c and v.tipo = :tipo
            where c.eleicao.id = :eleicaoId and c.cargo = :cargo
            group by c.id, c.nome, c.numero
            order by c.numero
            """)
    List<CandidatoComVotos> contarVotosPorCandidato(@Param("eleicaoId") Long eleicaoId,
                                                    @Param("cargo") Cargo cargo,
                                                    @Param("tipo") TipoVoto tipo);

    boolean existsByCandidatoId(Long candidatoId);

}
