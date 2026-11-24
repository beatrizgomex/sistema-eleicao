package br.com.eleicao.sistema.votacao.model;

import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.auth.model.Usuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "comprovante_votacao", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"usuario_id", "eleicao_id"})
})
@Getter @Setter @NoArgsConstructor
public class ComprovanteVotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "eleicao_id", nullable = false)
    private Eleicao eleicao;

    @Column(nullable = false, unique = true, length = 19)
    private String codigo;

    @Column(nullable = false)
    private LocalDateTime emitidoEm;
}
