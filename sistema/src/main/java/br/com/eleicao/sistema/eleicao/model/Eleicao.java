
package br.com.eleicao.sistema.eleicao.model;

import br.com.eleicao.sistema.shared.enums.SituacaoEleicao;
import br.com.eleicao.sistema.shared.enums.Turno;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "eleicao")
@Getter
@Setter
@NoArgsConstructor
public class Eleicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Turno turno;

    @Column(nullable = false)
    private LocalTime horaAbertura;

    @Column(nullable = false)
    private LocalTime horaEncerramento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoEleicao situacao = SituacaoEleicao.AGENDADA;
}
