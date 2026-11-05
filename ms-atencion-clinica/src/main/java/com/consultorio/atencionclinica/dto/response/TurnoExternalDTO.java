package com.consultorio.atencionclinica.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO externo que modela la respuesta del endpoint GET /api/turnos/{turnoId}
 * de ms-agenda-usuarios.
 *
 * Solo se mapean los campos que ms-atencion-clinica necesita para sus validaciones:
 *   - Verificar que el turno exista (si el cliente devuelve 404 → ResourceNotFoundException).
 *   - Verificar que pacienteId y odontologoId coincidan con los del request (RN-10).
 *   - Verificar que el estado sea compatible con el registro de prácticas/evoluciones.
 *
 * Campos adicionales que devuelva ms-agenda-usuarios son ignorados por Jackson.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoExternalDTO {

    private Long id;
    private Long pacienteId;
    private Long odontologoId;

    /**
     * Estado del turno: "PROGRAMADO", "ATENDIDO", "INASISTIDO", "CANCELADO".
     * Los estados válidos para registrar prácticas se validan en TurnoPracticaRealizadaService.
     */
    private String estado;
}
