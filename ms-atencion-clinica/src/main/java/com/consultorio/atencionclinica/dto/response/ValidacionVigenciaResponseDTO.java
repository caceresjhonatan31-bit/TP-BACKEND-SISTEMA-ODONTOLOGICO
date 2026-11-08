package com.consultorio.atencionclinica.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidacionVigenciaResponseDTO {

    private Long id;

    private Boolean vigente;

    private String detalle;
}
