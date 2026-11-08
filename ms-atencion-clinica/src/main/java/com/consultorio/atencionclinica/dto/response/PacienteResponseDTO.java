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
public class PacienteResponseDTO {

    private Long id;
    private String dni;
    private String nombre;
    private String apellido;
    private String telefono;
    private String email;
    private Boolean activo;
    private LocalDateTime fechaAlta;
}
