package com.consultorio.atencionclinica.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoExternalDTO {

    private Long id;
    private Long pacienteId;
    private Long odontologoId;
    private String estado;
}
