package br.com.eleicao.sistema.votacao.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "voto")
@Getter @Setter @NoArgsConstructor
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}