package br.com.eleicao.sistema.votacao.model;

import br.com.eleicao.sistema.candidato.model.Candidato;
import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.shared.enums.Cargo;
import br.com.eleicao.sistema.shared.enums.TipoVoto;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "voto")
@Getter @Setter @NoArgsConstructor
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "eleicao_id", nullable = false)
    private Eleicao eleicao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Cargo cargo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVoto tipo;

    @ManyToOne
    @JoinColumn(name = "candidato_id")
    private Candidato candidato;
}
