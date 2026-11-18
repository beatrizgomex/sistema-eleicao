
package br.com.eleicao.sistema.eleicao.dto;

import br.com.eleicao.sistema.eleicao.model.Eleicao;
import br.com.eleicao.sistema.shared.enums.SituacaoEleicao;
import br.com.eleicao.sistema.shared.enums.Turno;

import java.time.LocalDate;
import java.time.LocalTime;

public record EleicaoResponseDTO(
        Long id,
        LocalDate data,
        Turno turno,
        LocalTime horaAbertura,
        LocalTime horaEncerramento,
        SituacaoEleicao situacao
) {
    public static EleicaoResponseDTO fromEntity(Eleicao eleicao) {
        return new EleicaoResponseDTO(
                eleicao.getId(),
                eleicao.getData(),
                eleicao.getTurno(),
                eleicao.getHoraAbertura(),
                eleicao.getHoraEncerramento(),
                eleicao.getSituacao()
        );
    }
}
