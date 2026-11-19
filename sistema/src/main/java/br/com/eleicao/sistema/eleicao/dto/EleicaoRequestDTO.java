
package br.com.eleicao.sistema.eleicao.dto;

import br.com.eleicao.sistema.shared.enums.Turno;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record EleicaoRequestDTO(

        @NotNull(message = "A data é obrigatória")
        LocalDate data,

        @NotNull(message = "O turno é obrigatório")
        Turno turno,

        @NotNull(message = "A hora de abertura é obrigatória")
        LocalTime horaAbertura,

        @NotNull(message = "A hora de encerramento é obrigatória")
        LocalTime horaEncerramento

) {}
