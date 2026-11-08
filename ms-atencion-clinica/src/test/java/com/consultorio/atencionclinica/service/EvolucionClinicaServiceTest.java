package com.consultorio.atencionclinica.service;

import com.consultorio.atencionclinica.client.AgendaUsuariosClient;
import com.consultorio.atencionclinica.dto.request.EvolucionClinicaRequestDTO;
import com.consultorio.atencionclinica.dto.response.EvolucionClinicaResponseDTO;
import com.consultorio.atencionclinica.dto.response.TurnoExternalDTO;
import com.consultorio.atencionclinica.model.EvolucionClinica;
import com.consultorio.atencionclinica.model.Paciente;
import com.consultorio.atencionclinica.repository.EvolucionClinicaRepository;
import com.consultorio.atencionclinica.repository.PacienteRepository;
import com.consultorio.atencionclinica.repository.TurnoPracticaRealizadaRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvolucionClinicaServiceTest {

    @Mock
    private EvolucionClinicaRepository evolucionClinicaRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private TurnoPracticaRealizadaRepository turnoPracticaRealizadaRepository;

    @Mock
    private AgendaUsuariosClient agendaUsuariosClient;

    @InjectMocks
    private EvolucionClinicaService service;

    private EvolucionClinicaRequestDTO requestDTO;
    private Paciente paciente;
    private TurnoExternalDTO turnoExternalDesde;
    private TurnoExternalDTO turnoExternalHasta;

    @BeforeEach
    void setUp() {
        requestDTO = EvolucionClinicaRequestDTO.builder()
                .pacienteId(10L)
                .turnoDesdeId(100L)
                .turnoHastaId(105L)
                .actualizadoPorOdontologoId(5L)
                .diagnostico("Gingivitis moderada")
                .evolucionTexto("Se realiza tartrectomía y se indica enjuague con clorhexidina.")
                .build();

        paciente = new Paciente();
        paciente.setId(10L);
        paciente.setDni("35111222");
        paciente.setNombre("Lucia");
        paciente.setApellido("Fernandez");
        paciente.setActivo(true);

        turnoExternalDesde = TurnoExternalDTO.builder()
                .id(100L)
                .pacienteId(10L)
                .odontologoId(5L)
                .estado("ATENDIDO")
                .build();

        turnoExternalHasta = TurnoExternalDTO.builder()
                .id(105L)
                .pacienteId(10L)
                .odontologoId(5L)
                .estado("ATENDIDO")
                .build();
    }

    @Test
    @DisplayName("Registro exitoso de evolución clínica")
    void registrarEvolucion_exitoso() {
        when(pacienteRepository.findById(10L)).thenReturn(Optional.of(paciente));
        when(agendaUsuariosClient.obtenerTurno(100L)).thenReturn(turnoExternalDesde);
        when(agendaUsuariosClient.obtenerTurno(105L)).thenReturn(turnoExternalHasta);
        when(evolucionClinicaRepository.save(any(EvolucionClinica.class))).thenAnswer(invocation -> {
            EvolucionClinica ec = invocation.getArgument(0);
            ec.setId(1L);
            ec.setFechaActualizacion(LocalDateTime.now());
            return ec;
        });

        EvolucionClinicaResponseDTO response = service.registrarEvolucion(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.getPacienteId()).isEqualTo(10L);
        assertThat(response.getTurnoDesdeId()).isEqualTo(100L);
        assertThat(response.getTurnoHastaId()).isEqualTo(105L);
        assertThat(response.getDiagnostico()).isEqualTo("Gingivitis moderada");
        assertThat(response.getNombreCompletoPaciente()).isEqualTo("Fernandez, Lucia");
        verify(evolucionClinicaRepository).save(any(EvolucionClinica.class));
    }

    @Test
    @DisplayName("Rechazo por regla RN-15 cuando turnoHastaId < turnoDesdeId")
    void registrarEvolucion_rangoIncoherente_lanzaConflictException() {
        requestDTO.setTurnoDesdeId(200L);
        requestDTO.setTurnoHastaId(150L); // Menor que turnoDesdeId

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.registrarEvolucion(requestDTO));

        assertThat(ex.getMessage()).contains("según RN-15");
        assertThat(ex.getMessage()).contains("turnoHastaId=150");
        verify(pacienteRepository, never()).findById(any());
        verify(agendaUsuariosClient, never()).obtenerTurno(any());
        verify(evolucionClinicaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Control de confidencialidad médica (RN-16): ADMIN accede a la historia clínica")
    void listarPorPaciente_comoAdmin_permitido() {
        when(pacienteRepository.existsById(10L)).thenReturn(true);
        EvolucionClinica ec = new EvolucionClinica();
        ec.setPaciente(paciente);
        ec.setTurnoDesdeId(100L);
        ec.setDiagnostico("Diagnóstico 1");
        ec.setEvolucionTexto("Nota 1");
        when(evolucionClinicaRepository.findByPacienteIdOrderByFechaActualizacionDesc(10L))
                .thenReturn(List.of(ec));

        List<EvolucionClinicaResponseDTO> result = service.listarPorPaciente(10L, 999L, true);

        assertThat(result).hasSize(1);
        verify(evolucionClinicaRepository).findByPacienteIdOrderByFechaActualizacionDesc(10L);
    }

    @Test
    @DisplayName("Control de confidencialidad médica (RN-16): Odontólogo que atendió previamente accede a la historia clínica")
    void listarPorPaciente_odontologoTratante_permitido() {
        when(pacienteRepository.existsById(10L)).thenReturn(true);
        when(evolucionClinicaRepository.existsByPacienteIdAndActualizadoPorOdontologoId(10L, 5L)).thenReturn(true);

        EvolucionClinica ec = new EvolucionClinica();
        ec.setPaciente(paciente);
        ec.setTurnoDesdeId(100L);
        ec.setDiagnostico("Diagnóstico 1");
        ec.setEvolucionTexto("Nota 1");
        when(evolucionClinicaRepository.findByPacienteIdOrderByFechaActualizacionDesc(10L))
                .thenReturn(List.of(ec));

        List<EvolucionClinicaResponseDTO> result = service.listarPorPaciente(10L, 5L, false);

        assertThat(result).hasSize(1);
        verify(evolucionClinicaRepository).findByPacienteIdOrderByFechaActualizacionDesc(10L);
    }

    @Test
    @DisplayName("Control de confidencialidad médica (RN-16): Odontólogo que no atendió al paciente es rechazado")
    void listarPorPaciente_odontologoNoTratante_bloqueadoConConflictException() {
        when(pacienteRepository.existsById(10L)).thenReturn(true);
        when(evolucionClinicaRepository.existsByPacienteIdAndActualizadoPorOdontologoId(10L, 77L)).thenReturn(false);
        when(turnoPracticaRealizadaRepository.existsByPacienteIdAndOdontologoId(10L, 77L)).thenReturn(false);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.listarPorPaciente(10L, 77L, false));

        assertThat(ex.getMessage()).contains("Acceso denegado");
        assertThat(ex.getMessage()).contains("RN-16");
        verify(evolucionClinicaRepository, never()).findByPacienteIdOrderByFechaActualizacionDesc(any());
    }
}
