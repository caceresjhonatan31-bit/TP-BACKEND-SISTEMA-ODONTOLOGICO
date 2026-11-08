package com.consultorio.atencionclinica.service;

import com.consultorio.atencionclinica.dto.request.PracticaOdontologicaRequestDTO;
import com.consultorio.atencionclinica.dto.response.PracticaOdontologicaResponseDTO;
import com.consultorio.atencionclinica.dto.response.ValidacionVigenciaResponseDTO;
import com.consultorio.atencionclinica.model.PracticaOdontologica;
import com.consultorio.atencionclinica.repository.PracticaOdontologicaRepository;
import com.consultorio.common.exception.ConflictException;
import com.consultorio.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PracticaOdontologicaServiceTest {

    @Mock
    private PracticaOdontologicaRepository practicaRepository;

    @InjectMocks
    private PracticaOdontologicaService practicaService;

    private PracticaOdontologicaRequestDTO requestDTO;
    private PracticaOdontologica practica;

    @BeforeEach
    void setUp() {
        requestDTO = PracticaOdontologicaRequestDTO.builder()
                .codigoNomenclador("01.01")
                .nombre("Consulta odontológica general")
                .descripcion("Revisión bucodental y diagnóstico")
                .build();

        practica = new PracticaOdontologica();
        practica.setId(1L);
        practica.setCodigoNomenclador("01.01");
        practica.setNombre("Consulta odontológica general");
        practica.setDescripcion("Revisión bucodental y diagnóstico");
        practica.setActivo(true);
    }

    @Test
    @DisplayName("Alta exitosa de práctica odontológica")
    void crearPractica_exitoso() {
        when(practicaRepository.existsByCodigoNomenclador("01.01")).thenReturn(false);
        when(practicaRepository.save(any(PracticaOdontologica.class))).thenAnswer(invocation -> {
            PracticaOdontologica p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        PracticaOdontologicaResponseDTO response = practicaService.crearPractica(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.getCodigoNomenclador()).isEqualTo("01.01");
        assertThat(response.getNombre()).isEqualTo("Consulta odontológica general");
        assertThat(response.getActivo()).isTrue();
        verify(practicaRepository).save(any(PracticaOdontologica.class));
    }

    @Test
    @DisplayName("Rechazo por código nomenclador duplicado arrojando ConflictException")
    void crearPractica_codigoDuplicado_lanzaConflictException() {
        when(practicaRepository.existsByCodigoNomenclador("01.01")).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> practicaService.crearPractica(requestDTO));

        assertThat(ex.getMessage()).contains("Ya existe una práctica con el código nomenclador: 01.01");
        verify(practicaRepository, never()).save(any(PracticaOdontologica.class));
    }

    @Test
    @DisplayName("Inhabilitación / baja lógica exitosa de práctica")
    void darDeBaja_exitoso() {
        when(practicaRepository.findById(1L)).thenReturn(Optional.of(practica));
        when(practicaRepository.save(any(PracticaOdontologica.class))).thenReturn(practica);

        practicaService.darDeBaja(1L);

        assertThat(practica.getActivo()).isFalse();
        verify(practicaRepository).save(practica);
    }

    @Test
    @DisplayName("Rechazo de baja lógica si la práctica ya estaba inhabilitada")
    void darDeBaja_yaInhabilitada_lanzaConflictException() {
        practica.setActivo(false);
        when(practicaRepository.findById(1L)).thenReturn(Optional.of(practica));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> practicaService.darDeBaja(1L));

        assertThat(ex.getMessage()).contains("ya está inhabilitada");
        verify(practicaRepository, never()).save(practica);
    }

    @Test
    @DisplayName("Reactivación / habilitación exitosa de práctica")
    void reactivarPractica_exitoso() {
        practica.setActivo(false);
        when(practicaRepository.findById(1L)).thenReturn(Optional.of(practica));
        when(practicaRepository.save(any(PracticaOdontologica.class))).thenReturn(practica);

        PracticaOdontologicaResponseDTO response = practicaService.reactivarPractica(1L);

        assertThat(response.getActivo()).isTrue();
        assertThat(practica.getActivo()).isTrue();
        verify(practicaRepository).save(practica);
    }

    @Test
    @DisplayName("Validación inter-servicio: práctica vigente (activo = true)")
    void validarPracticaActiva_practicaVigente() {
        practica.setActivo(true);
        when(practicaRepository.findById(1L)).thenReturn(Optional.of(practica));

        ValidacionVigenciaResponseDTO response = practicaService.validarPracticaActiva(1L);

        assertThat(response.getVigente()).isTrue();
        assertThat(response.getDetalle()).contains("Práctica vigente");
    }

    @Test
    @DisplayName("Validación inter-servicio: práctica inhabilitada (activo = false)")
    void validarPracticaActiva_practicaInhabilitada() {
        practica.setActivo(false);
        when(practicaRepository.findById(1L)).thenReturn(Optional.of(practica));

        ValidacionVigenciaResponseDTO response = practicaService.validarPracticaActiva(1L);

        assertThat(response.getVigente()).isFalse();
        assertThat(response.getDetalle()).contains("Práctica inhabilitada");
    }

    @Test
    @DisplayName("Validación inter-servicio: práctica inexistente")
    void validarPracticaActiva_practicaNoExiste() {
        when(practicaRepository.findById(999L)).thenReturn(Optional.empty());

        ValidacionVigenciaResponseDTO response = practicaService.validarPracticaActiva(999L);

        assertThat(response.getVigente()).isFalse();
        assertThat(response.getDetalle()).contains("No existe ninguna práctica registrada");
    }
}
