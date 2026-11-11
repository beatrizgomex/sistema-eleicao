package br.com.eleicao.sistema.candidato.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "partido")
@Getter @Setter @NoArgsConstructor
public class Partido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}