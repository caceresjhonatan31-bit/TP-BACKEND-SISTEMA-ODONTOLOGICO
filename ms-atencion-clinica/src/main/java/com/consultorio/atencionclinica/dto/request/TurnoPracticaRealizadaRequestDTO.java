package com.consultorio.atencionclinica.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de entrada para registrar una práctica efectivamente realizada durante una consulta.
 *
 * RN-10: La práctica referenciada por practicaId debe tener activo = true.
 * RN-11: El odontólogo registra las prácticas realizadas durante la atención.
 * RN-16: odontologoId es clave para la validación de acceso a la historia clínica.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoPracticaRealizadaRequestDTO {

    @NotNull(message = "El ID del turno es obligatorio")
    @Positive(message = "El ID del turno debe ser un número positivo")
    private Long turnoId;

    @NotNull(message = "El ID del paciente es obligatorio")
    @Positive(message = "El ID del paciente debe ser un número positivo")
    private Long pacienteId;

    @NotNull(message = "El ID de la práctica es obligatorio")
    @Positive(message = "El ID de la práctica debe ser un número positivo")
    private Long practicaId;

    @NotNull(message = "El ID del odontólogo es obligatorio")
    @Positive(message = "El ID del odontólogo debe ser un número positivo")
    private Long odontologoId;

    /** Observación libre: pieza tratada, anestesia utilizada, etc. Campo opcional. */
    private String notaAclaratoria;
}
