package com.consultorio.atencionclinica.service;

import com.consultorio.atencionclinica.client.AgendaUsuariosClient;
import com.consultorio.atencionclinica.dto.request.TurnoPracticaRealizadaRequestDTO;
import com.consultorio.atencionclinica.dto.response.TurnoExternalDTO;
import com.consultorio.atencionclinica.dto.response.TurnoPracticaRealizadaResponseDTO;
import com.consultorio.atencionclinica.model.Paciente;
import com.consultorio.atencionclinica.model.PracticaOdontologica;
import com.consultorio.atencionclinica.model.TurnoPracticaRealizada;
import com.consultorio.atencionclinica.repository.EvolucionClinicaRepository;
import com.consultorio.atencionclinica.repository.PacienteRepository;
import com.consultorio.atencionclinica.repository.PracticaOdontologicaRepository;
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
class TurnoPracticaRealizadaServiceTest {

    @Mock
    private TurnoPracticaRealizadaRepository turnoPracticaRealizadaRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private PracticaOdontologicaRepository practicaRepository;

    @Mock
    private EvolucionClinicaRepository evolucionClinicaRepository;

    @Mock
    private AgendaUsuariosClient agendaUsuariosClient;

    @InjectMocks
    private TurnoPracticaRealizadaService service;

    private TurnoPracticaRealizadaRequestDTO requestDTO;
    private Paciente paciente;
    private PracticaOdontologica practica;
    private TurnoExternalDTO turnoExternal;

    @BeforeEach
    void setUp() {
        requestDTO = TurnoPracticaRealizadaRequestDTO.builder()
                .turnoId(100L)
                .pacienteId(10L)
                .practicaId(20L)
                .odontologoId(5L)
                .notaAclaratoria("Se realizó limpieza con ultrasonido")
                .build();

        paciente = new Paciente();
        paciente.setId(10L);
        paciente.setDni("35111222");
        paciente.setNombre("Lucia");
        paciente.setApellido("Fernandez");
        paciente.setActivo(true);

        practica = new PracticaOdontologica();
        practica.setId(20L);
        practica.setCodigoNomenclador("02.01");
        practica.setNombre("Limpieza dental");
        practica.setActivo(true);

        turnoExternal = TurnoExternalDTO.builder()
                .id(100L)
                .pacienteId(10L)
                .odontologoId(5L)
                .estado("PROGRAMADO")
                .build();
    }

    @Test
    @DisplayName("Registro exitoso de práctica efectuada validando contra mock de AgendaUsuariosClient")
    void registrarPracticaRealizada_exitoso() {
        when(pacienteRepository.findById(10L)).thenReturn(Optional.of(paciente));
        when(practicaRepository.findById(20L)).thenReturn(Optional.of(practica));
        when(agendaUsuariosClient.obtenerTurno(100L)).thenReturn(turnoExternal);
        when(turnoPracticaRealizadaRepository.save(any(TurnoPracticaRealizada.class))).thenAnswer(invocation -> {
            TurnoPracticaRealizada tpr = invocation.getArgument(0);
            tpr.setId(1L);
            tpr.setFechaRegistro(LocalDateTime.now());
            return tpr;
        });

        TurnoPracticaRealizadaResponseDTO response = service.registrarPracticaRealizada(requestDTO);

        assertThat(response).isNotNull();
        assertThat(response.getTurnoId()).isEqualTo(100L);
        assertThat(response.getPacienteId()).isEqualTo(10L);
        assertThat(response.getCodigoNomenclador()).isEqualTo("02.01");
        assertThat(response.getNombrePractica()).isEqualTo("Limpieza dental");
        assertThat(response.getNotaAclaratoria()).isEqualTo("Se realizó limpieza con ultrasonido");
        verify(turnoPracticaRealizadaRepository).save(any(TurnoPracticaRealizada.class));
    }

    @Test
    @DisplayName("Rechazo si la práctica no está activa (RN-10 y RN-11)")
    void registrarPracticaRealizada_practicaInactiva_lanzaConflictException() {
        practica.setActivo(false);
        when(pacienteRepository.findById(10L)).thenReturn(Optional.of(paciente));
        when(practicaRepository.findById(20L)).thenReturn(Optional.of(practica));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.registrarPracticaRealizada(requestDTO));

        assertThat(ex.getMessage()).contains("está inhabilitada (RN-10)");
        verify(agendaUsuariosClient, never()).obtenerTurno(any());
        verify(turnoPracticaRealizadaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechazo si el paciente se encuentra inactivo")
    void registrarPracticaRealizada_pacienteInactivo_lanzaConflictException() {
        paciente.setActivo(false);
        when(pacienteRepository.findById(10L)).thenReturn(Optional.of(paciente));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.registrarPracticaRealizada(requestDTO));

        assertThat(ex.getMessage()).contains("está dado de baja");
        verify(practicaRepository, never()).findById(any());
        verify(turnoPracticaRealizadaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rechazo si el turno se encuentra en estado CANCELADO o INASISTIDO")
    void registrarPracticaRealizada_turnoCancelado_lanzaConflictException() {
        turnoExternal.setEstado("CANCELADO");
        when(pacienteRepository.findById(10L)).thenReturn(Optional.of(paciente));
        when(practicaRepository.findById(20L)).thenReturn(Optional.of(practica));
        when(agendaUsuariosClient.obtenerTurno(100L)).thenReturn(turnoExternal);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.registrarPracticaRealizada(requestDTO));

        assertThat(ex.getMessage()).contains("El turno debe encontrarse en estado PROGRAMADO o ATENDIDO");
        verify(turnoPracticaRealizadaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Control de confidencialidad médica (RN-16): ADMIN tiene acceso total")
    void listarPorPaciente_comoAdmin_permitido() {
        when(pacienteRepository.existsById(10L)).thenReturn(true);
        TurnoPracticaRealizada tpr = new TurnoPracticaRealizada();
        tpr.setTurnoId(100L);
        tpr.setPaciente(paciente);
        tpr.setPractica(practica);
        tpr.setOdontologoId(5L);
        when(turnoPracticaRealizadaRepository.findByPacienteId(10L)).thenReturn(List.of(tpr));

        List<TurnoPracticaRealizadaResponseDTO> result = service.listarPorPaciente(10L, 999L, true);

        assertThat(result).hasSize(1);
        verify(turnoPracticaRealizadaRepository).findByPacienteId(10L);
    }

    @Test
    @DisplayName("Control de confidencialidad médica (RN-16): Odontólogo tratante previo tiene acceso")
    void listarPorPaciente_odontologoTratante_permitido() {
        when(pacienteRepository.existsById(10L)).thenReturn(true);
        when(turnoPracticaRealizadaRepository.existsByPacienteIdAndOdontologoId(10L, 5L)).thenReturn(true);

        TurnoPracticaRealizada tpr = new TurnoPracticaRealizada();
        tpr.setTurnoId(100L);
        tpr.setPaciente(paciente);
        tpr.setPractica(practica);
        tpr.setOdontologoId(5L);
        when(turnoPracticaRealizadaRepository.findByPacienteId(10L)).thenReturn(List.of(tpr));

        List<TurnoPracticaRealizadaResponseDTO> result = service.listarPorPaciente(10L, 5L, false);

        assertThat(result).hasSize(1);
        verify(turnoPracticaRealizadaRepository).findByPacienteId(10L);
    }

    @Test
    @DisplayName("Control de confidencialidad médica (RN-16): Tercero no tratante es bloqueado")
    void listarPorPaciente_odontologoNoTratante_bloqueadoConConflictException() {
        when(pacienteRepository.existsById(10L)).thenReturn(true);
        when(turnoPracticaRealizadaRepository.existsByPacienteIdAndOdontologoId(10L, 88L)).thenReturn(false);
        when(evolucionClinicaRepository.existsByPacienteIdAndActualizadoPorOdontologoId(10L, 88L)).thenReturn(false);

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.listarPorPaciente(10L, 88L, false));

        assertThat(ex.getMessage()).contains("Acceso denegado");
        assertThat(ex.getMessage()).contains("RN-16");
        verify(turnoPracticaRealizadaRepository, never()).findByPacienteId(any());
    }
}
