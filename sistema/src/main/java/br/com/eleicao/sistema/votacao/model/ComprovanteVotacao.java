package br.com.eleicao.sistema.votacao.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "comprovante_votacao")
@Getter @Setter @NoArgsConstructor
public class ComprovanteVotacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}