package com.consultorio.atencionclinica.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoPracticaRealizadaResponseDTO {

    private Long id;
    private Long turnoId;
    private Long pacienteId;
    private Long practicaId;
    private String codigoNomenclador;
    private String nombrePractica;
    private Long odontologoId;
    private String notaAclaratoria;
    private LocalDateTime fechaRegistro;
}
