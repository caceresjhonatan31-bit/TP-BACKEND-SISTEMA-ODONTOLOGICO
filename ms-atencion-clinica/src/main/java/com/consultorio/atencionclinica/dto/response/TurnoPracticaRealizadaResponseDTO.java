package com.consultorio.atencionclinica.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de salida de una práctica efectivamente realizada durante una consulta.
 * Incluye el código y nombre de la práctica (desnormalizados) para lectura
 * directa sin necesidad de un segundo request.
 *
 * RN-16: solo visible a ADMIN o al odontólogo que atendió al paciente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoPracticaRealizadaResponseDTO {

    private Long id;
    private Long turnoId;
    private Long pacienteId;
    private Long practicaId;
    /** Código nomenclador de la práctica realizada (desnormalizado). */
    private String codigoNomenclador;
    /** Nombre de la práctica realizada (desnormalizado). */
    private String nombrePractica;
    private Long odontologoId;
    private String notaAclaratoria;
    private LocalDateTime fechaRegistro;
}
