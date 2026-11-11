package br.com.eleicao.sistema.candidato.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "candidato")
@Getter @Setter @NoArgsConstructor
public class Candidato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}