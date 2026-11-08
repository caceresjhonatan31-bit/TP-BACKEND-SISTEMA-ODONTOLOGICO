package com.consultorio.atencionclinica.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionClinicaRequestDTO {

    @NotNull(message = "El ID del paciente es obligatorio")
    @Positive(message = "El ID del paciente debe ser un número positivo")
    private Long pacienteId;

    @NotNull(message = "El ID del turno de inicio es obligatorio")
    @Positive(message = "El ID del turno de inicio debe ser un número positivo")
    private Long turnoDesdeId;

    /**
     * Nullable: NULL = sesión única o tratamiento en curso (RN-15).
     * Si se envía debe ser >= turnoDesdeId (validado en servicio + CHECK en DB).
     */
    @Positive(message = "El ID del turno de cierre debe ser un número positivo")
    private Long turnoHastaId;

    @NotNull(message = "El ID del odontólogo actualizador es obligatorio")
    @Positive(message = "El ID del odontólogo debe ser un número positivo")
    private Long actualizadoPorOdontologoId;

    @NotBlank(message = "El diagnóstico es obligatorio")
    @Size(max = 255, message = "El diagnóstico no puede superar los 255 caracteres")
    private String diagnostico;

    @NotBlank(message = "El texto de evolución clínica es obligatorio")
    private String evolucionTexto;
}
