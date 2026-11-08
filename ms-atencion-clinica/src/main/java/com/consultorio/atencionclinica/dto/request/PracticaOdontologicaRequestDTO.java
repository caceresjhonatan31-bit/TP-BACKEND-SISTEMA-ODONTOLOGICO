package com.consultorio.atencionclinica.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticaOdontologicaRequestDTO {

    @NotBlank(message = "El código nomenclador es obligatorio")
    @Size(max = 20, message = "El código nomenclador no puede superar los 20 caracteres")
    private String codigoNomenclador;

    @NotBlank(message = "El nombre de la práctica es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
    private String nombre;

    /** Descripción clínica de la práctica. Campo opcional. */
    private String descripcion;
}
