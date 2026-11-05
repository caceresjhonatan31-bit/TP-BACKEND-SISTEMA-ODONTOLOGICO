package com.consultorio.atencionclinica.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para los endpoints de validación inter-servicio.
 * Consumido por ms-agenda-usuarios para verificar antes de asignar un turno:
 *   - GET /api/pacientes/{id}/validar-activo
 *   - GET /api/practicas/{id}/validar-activa
 *
 * El campo `vigente` es el booleano que el cliente valida.
 * El campo `detalle` explica el motivo cuando vigente = false.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionVigenciaResponseDTO {

    /** ID del recurso consultado (paciente o práctica). */
    private Long id;

    /**
     * true  → el recurso existe y está activo (puede usarse en un turno).
     * false → el recurso está inactivo o no existe.
     */
    private Boolean vigente;

    /**
     * Mensaje descriptivo del resultado.
     * Ej: "Paciente activo", "Paciente dado de baja", "Práctica vigente", etc.
     */
    private String detalle;
}
