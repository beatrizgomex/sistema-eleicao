package br.com.eleicao.sistema.eleitor.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "eleitor")
@Getter @Setter @NoArgsConstructor
public class Eleitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}