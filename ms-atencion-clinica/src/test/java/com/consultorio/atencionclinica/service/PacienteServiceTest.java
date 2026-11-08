package com.consultorio.atencionclinica.service;

import com.consultorio.atencionclinica.dto.request.PacienteRequestDTO;
import com.consultorio.atencionclinica.dto.response.PacienteResponseDTO;
import com.consultorio.atencionclinica.dto.response.ValidacionVigenciaResponseDTO;
import com.consultorio.atencionclinica.model.Paciente;
import com.consultorio.atencionclinica.repository.PacienteRepository;
import com.consultorio.common.exception.ConflictException;
import com.consultorio.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private PacienteService pacienteService;

    private PacienteRequestDTO pacienteRequestDTO;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        pacienteRequestDTO = PacienteRequestDTO.builder()
                .dni("40123456")
                .nombre("Martin")
                .apellido("Gomez")
                .telefono("3511234567")
                .email("martin.gomez@test.com")
                .build();

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setDni("40123456");
        paciente.setNombre("Martin");
        paciente.setApellido("Gomez");
        paciente.setTelefono("3511234567");
        paciente.setEmail("martin.gomez@test.com");
        paciente.setActivo(true);
        paciente.setFechaAlta(LocalDateTime.now());
    }

    @Test
    @DisplayName("Alta exitosa de paciente cuando el DNI no existe")
    void crearPaciente_exitoso() {
        when(pacienteRepository.existsByDni("40123456")).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> {
            Paciente p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        PacienteResponseDTO response = pacienteService.crearPaciente(pacienteRequestDTO);

        assertThat(response).isNotNull();
        assertThat(response.getDni()).isEqualTo("40123456");
        assertThat(response.getNombre()).isEqualTo("Martin");
        assertThat(response.getActivo()).isTrue();
        verify(pacienteRepository).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Rechazo por DNI duplicado arrojando ConflictException")
    void crearPaciente_dniDuplicado_lanzaConflictException() {
        when(pacienteRepository.existsByDni("40123456")).thenReturn(true);
        when(pacienteRepository.findByDni("40123456")).thenReturn(Optional.of(paciente));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> pacienteService.crearPaciente(pacienteRequestDTO));

        assertThat(ex.getMessage()).contains("Ya existe un paciente registrado con el DNI indicado");
        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Baja lógica (activo = false) exitosa")
    void darDeBaja_exitoso() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class))).thenReturn(paciente);

        pacienteService.darDeBaja(1L);

        assertThat(paciente.getActivo()).isFalse();
        verify(pacienteRepository).save(paciente);
    }

    @Test
    @DisplayName("Rechazo de baja lógica si el paciente ya estaba inactivo")
    void darDeBaja_yaInactivo_lanzaConflictException() {
        paciente.setActivo(false);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> pacienteService.darDeBaja(1L));

        assertThat(ex.getMessage()).contains("ya está dado de baja");
        verify(pacienteRepository, never()).save(paciente);
    }

    @Test
    @DisplayName("Reactivación exitosa de paciente dado de baja (RN-13)")
    void reactivarPaciente_exitoso() {
        paciente.setActivo(false);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class))).thenReturn(paciente);

        PacienteResponseDTO response = pacienteService.reactivarPaciente(1L);

        assertThat(response.getActivo()).isTrue();
        assertThat(paciente.getActivo()).isTrue();
        verify(pacienteRepository).save(paciente);
    }

    @Test
    @DisplayName("Rechazo de reactivación si el paciente ya se encuentra activo (RN-13)")
    void reactivarPaciente_yaActivo_lanzaConflictException() {
        paciente.setActivo(true);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> pacienteService.reactivarPaciente(1L));

        assertThat(ex.getMessage()).contains("ya se encuentra activo");
        verify(pacienteRepository, never()).save(paciente);
    }

    @Test
    @DisplayName("Validación inter-servicio: paciente existe y está activo")
    void validarPacienteActivo_pacienteVigente() {
        paciente.setActivo(true);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        ValidacionVigenciaResponseDTO response = pacienteService.validarPacienteActivo(1L);

        assertThat(response.getVigente()).isTrue();
        assertThat(response.getDetalle()).contains("Paciente activo");
    }

    @Test
    @DisplayName("Validación inter-servicio: paciente existe pero está dado de baja")
    void validarPacienteActivo_pacienteInactivo() {
        paciente.setActivo(false);
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        ValidacionVigenciaResponseDTO response = pacienteService.validarPacienteActivo(1L);

        assertThat(response.getVigente()).isFalse();
        assertThat(response.getDetalle()).contains("dado de baja");
    }

    @Test
    @DisplayName("Validación inter-servicio: paciente no existe")
    void validarPacienteActivo_pacienteNoExiste() {
        when(pacienteRepository.findById(999L)).thenReturn(Optional.empty());

        ValidacionVigenciaResponseDTO response = pacienteService.validarPacienteActivo(999L);

        assertThat(response.getVigente()).isFalse();
        assertThat(response.getDetalle()).contains("No existe ningún paciente registrado");
    }
}
