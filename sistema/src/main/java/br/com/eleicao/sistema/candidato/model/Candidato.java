package br.com.eleicao.sistema.candidato.model;

import br.com.eleicao.sistema.auth.model.Usuario;
import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.candidato.model.Partido;
import br.com.eleicao.sistema.shared.enums.Cargo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "candidato", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"eleicao_id", "cargo", "numero"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Candidato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Cargo cargo;

    @ManyToOne
    @JoinColumn(name = "partido_id", nullable = false)
    private Partido partido;

    @ManyToOne
    @JoinColumn(name = "eleicao_id", nullable = false)
    private Eleicao eleicao;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id", nullable = false)
    private Usuario usuario;
}