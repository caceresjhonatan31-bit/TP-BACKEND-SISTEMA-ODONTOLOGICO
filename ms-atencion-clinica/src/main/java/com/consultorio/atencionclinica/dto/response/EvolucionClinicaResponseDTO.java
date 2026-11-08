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
public class EvolucionClinicaResponseDTO {

    private Long id;
    private Long pacienteId;
    //Nombre completo del paciente: "apellido, nombre"
    private String nombreCompletoPaciente;
    private Long turnoDesdeId;
    private Long turnoHastaId;
    private Long actualizadoPorOdontologoId;
    private String diagnostico;
    private String evolucionTexto;
    private LocalDateTime fechaActualizacion;
}
