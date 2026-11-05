package com.consultorio.atencionclinica.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de salida de una nota evolutiva clínica.
 * Incluye el nombre completo del paciente (desnormalizado) para evitar un
 * segundo round-trip al listar el historial clínico.
 *
 * RN-15: turnoHastaId puede ser null (sesión única o tratamiento en curso).
 * RN-16: este DTO solo se devuelve si el solicitante tiene acceso autorizado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionClinicaResponseDTO {

    private Long id;
    private Long pacienteId;
    /** Nombre completo del paciente: "apellido, nombre" */
    private String nombreCompletoPaciente;
    private Long turnoDesdeId;
    /** Nullable. NULL = sesión única o en curso (RN-15). */
    private Long turnoHastaId;
    private Long actualizadoPorOdontologoId;
    private String diagnostico;
    private String evolucionTexto;
    private LocalDateTime fechaActualizacion;
}
