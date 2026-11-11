package br.com.eleicao.sistema.eleicao.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "eleicao")
@Getter @Setter @NoArgsConstructor
public class Eleicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}