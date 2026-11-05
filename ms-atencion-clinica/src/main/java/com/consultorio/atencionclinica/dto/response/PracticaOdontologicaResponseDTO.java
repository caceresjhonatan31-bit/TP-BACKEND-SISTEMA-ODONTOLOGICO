package com.consultorio.atencionclinica.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de salida con los datos de una práctica del nomenclador odontológico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticaOdontologicaResponseDTO {

    private Long id;
    private String codigoNomenclador;
    private String nombre;
    private String descripcion;
    private Boolean activo;
}
