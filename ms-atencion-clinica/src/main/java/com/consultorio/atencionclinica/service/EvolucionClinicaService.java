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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para la gestión de notas y evoluciones clínicas.
 *
 * RN-15: Continuidad de sesiones clínicas (turnoHastaId >= turnoDesdeId).
 * RN-16: Confidencialidad en el acceso a diagnósticos y evoluciones médicas.
 */
@Slf4j
@Service
@Transactional
public class EvolucionClinicaService {

    private final EvolucionClinicaRepository evolucionClinicaRepository;
    private final PacienteRepository pacienteRepository;
    private final TurnoPracticaRealizadaRepository turnoPracticaRealizadaRepository;
    private final AgendaUsuariosClient agendaUsuariosClient;

    public EvolucionClinicaService(EvolucionClinicaRepository evolucionClinicaRepository,
                                   PacienteRepository pacienteRepository,
                                   TurnoPracticaRealizadaRepository turnoPracticaRealizadaRepository,
                                   AgendaUsuariosClient agendaUsuariosClient) {
        this.evolucionClinicaRepository = evolucionClinicaRepository;
        this.pacienteRepository = pacienteRepository;
        this.turnoPracticaRealizadaRepository = turnoPracticaRealizadaRepository;
        this.agendaUsuariosClient = agendaUsuariosClient;
    }

    /**
     * Registra una nueva evolución clínica.
     * RN-15: Si turnoHastaId no es null, valida que turnoHastaId >= turnoDesdeId.
     * Verifica la correspondencia del turno en ms-agenda-usuarios.
     */
    public EvolucionClinicaResponseDTO registrarEvolucion(EvolucionClinicaRequestDTO dto) {
        log.debug("Iniciando validaciones para registrar evolución (paciente ID: {}, turnoDesdeId: {}, turnoHastaId: {})",
                dto.getPacienteId(), dto.getTurnoDesdeId(), dto.getTurnoHastaId());

        // 1. Validar RN-15: Rango cronológico coherente entre turnos
        validarRangoTurnos(dto.getTurnoDesdeId(), dto.getTurnoHastaId());

        // 2. Validar Paciente
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> {
                    log.warn("Rechazo de evolución clínica: Paciente ID {} no encontrado", dto.getPacienteId());
                    return new ResourceNotFoundException("No se encontró ningún paciente con ID: " + dto.getPacienteId());
                });

        // 3. Validar Turno en ms-agenda-usuarios
        TurnoExternalDTO turno = agendaUsuariosClient.obtenerTurno(dto.getTurnoDesdeId());
        if (!turno.getPacienteId().equals(dto.getPacienteId())) {
            log.warn("Rechazo de evolución clínica: Turno inicial ID {} pertenece a paciente ID {}, recibido ID {}",
                    dto.getTurnoDesdeId(), turno.getPacienteId(), dto.getPacienteId());
            throw new ConflictException(
                    "El turno inicial con ID " + dto.getTurnoDesdeId() + " pertenece al paciente ID " +
                    turno.getPacienteId() + ", pero se asoció con paciente ID " + dto.getPacienteId());
        }

        // Si se especificó turno de cierre, validarlo también
        if (dto.getTurnoHastaId() != null && !dto.getTurnoHastaId().equals(dto.getTurnoDesdeId())) {
            TurnoExternalDTO turnoHasta = agendaUsuariosClient.obtenerTurno(dto.getTurnoHastaId());
            if (!turnoHasta.getPacienteId().equals(dto.getPacienteId())) {
                log.warn("Rechazo de evolución clínica: Turno final ID {} pertenece a paciente ID {}, recibido ID {}",
                        dto.getTurnoHastaId(), turnoHasta.getPacienteId(), dto.getPacienteId());
                throw new ConflictException(
                        "El turno de cierre con ID " + dto.getTurnoHastaId() + " pertenece al paciente ID " +
                        turnoHasta.getPacienteId() + ", pero se asoció con paciente ID " + dto.getPacienteId());
            }
        }

        // 4. Persistir nota evolutiva
        EvolucionClinica ec = new EvolucionClinica();
        ec.setPaciente(paciente);
        ec.setTurnoDesdeId(dto.getTurnoDesdeId());
        ec.setTurnoHastaId(dto.getTurnoHastaId());
        ec.setActualizadoPorOdontologoId(dto.getActualizadoPorOdontologoId());
        ec.setDiagnostico(dto.getDiagnostico());
        ec.setEvolucionTexto(dto.getEvolucionTexto());

        EvolucionClinica guardada = evolucionClinicaRepository.save(ec);
        log.info("Evolución clínica registrada exitosamente con ID: {} para paciente ID: {}",
                guardada.getId(), guardada.getPaciente().getId());

        return toResponseDTO(guardada);
    }

    /**
     * Actualiza una nota evolutiva existente.
     * RN-15: Si se modifica el rango de turnos, valida coherencia.
     */
    public EvolucionClinicaResponseDTO actualizarEvolucion(Long id, EvolucionClinicaRequestDTO dto) {
        log.debug("Iniciando validación para actualizar evolución clínica ID: {}", id);

        EvolucionClinica ec = evolucionClinicaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Evolución clínica ID {} no encontrada para actualización", id);
                    return new ResourceNotFoundException("No se encontró ninguna evolución clínica con ID: " + id);
                });

        validarRangoTurnos(dto.getTurnoDesdeId(), dto.getTurnoHastaId());

        ec.setTurnoDesdeId(dto.getTurnoDesdeId());
        ec.setTurnoHastaId(dto.getTurnoHastaId());
        ec.setActualizadoPorOdontologoId(dto.getActualizadoPorOdontologoId());
        ec.setDiagnostico(dto.getDiagnostico());
        ec.setEvolucionTexto(dto.getEvolucionTexto());

        EvolucionClinica actualizada = evolucionClinicaRepository.save(ec);
        log.info("Evolución clínica ID {} actualizada exitosamente", actualizada.getId());

        return toResponseDTO(actualizada);
    }

    /**
     * Consulta el historial clínico completo de un paciente.
     * RN-16: Valida confidencialidad médica (solo ADMIN o el odontólogo que atendió al paciente).
     */
    @Transactional(readOnly = true)
    public List<EvolucionClinicaResponseDTO> listarPorPaciente(Long pacienteId, Long odontologoIdSolicitante, boolean esAdmin) {
        log.debug("Verificando autorización RN-16 para consultar historial clínico de paciente ID {} (odontologoId: {}, esAdmin: {})",
                pacienteId, odontologoIdSolicitante, esAdmin);

        if (!pacienteRepository.existsById(pacienteId)) {
            log.warn("Paciente ID {} no encontrado para consultar historial clínico", pacienteId);
            throw new ResourceNotFoundException("No se encontró ningún paciente con ID: " + pacienteId);
        }

        if (!esAdmin) {
            boolean atendio = evolucionClinicaRepository.existsByPacienteIdAndActualizadoPorOdontologoId(pacienteId, odontologoIdSolicitante)
                    || turnoPracticaRealizadaRepository.existsByPacienteIdAndOdontologoId(pacienteId, odontologoIdSolicitante);
            if (!atendio) {
                log.warn("Rechazo de acceso a historia clínica (RN-16): Odontólogo ID {} no registra atenciones con paciente ID {}",
                        odontologoIdSolicitante, pacienteId);
                throw new ConflictException(
                        "Acceso denegado: El odontólogo ID " + odontologoIdSolicitante +
                        " no ha atendido al paciente ID " + pacienteId +
                        " y no tiene acceso a su historia clínica confidencial (RN-16).");
            }
        }

        return evolucionClinicaRepository.findByPacienteIdOrderByFechaActualizacionDesc(pacienteId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void validarRangoTurnos(Long turnoDesdeId, Long turnoHastaId) {
        if (turnoHastaId != null && turnoHastaId < turnoDesdeId) {
            log.warn("Rechazo por regla RN-15: turnoHastaId ({}) es menor a turnoDesdeId ({})", turnoHastaId, turnoDesdeId);
            throw new ConflictException(
                    "El turno de cierre (turnoHastaId=" + turnoHastaId +
                    ") debe ser posterior o igual al turno de inicio (turnoDesdeId=" + turnoDesdeId +
                    ") según RN-15.");
        }
    }

    private EvolucionClinicaResponseDTO toResponseDTO(EvolucionClinica ec) {
        String nombreCompleto = ec.getPaciente().getApellido() + ", " + ec.getPaciente().getNombre();
        return EvolucionClinicaResponseDTO.builder()
                .id(ec.getId())
                .pacienteId(ec.getPaciente().getId())
                .nombreCompletoPaciente(nombreCompleto)
                .turnoDesdeId(ec.getTurnoDesdeId())
                .turnoHastaId(ec.getTurnoHastaId())
                .actualizadoPorOdontologoId(ec.getActualizadoPorOdontologoId())
                .diagnostico(ec.getDiagnostico())
                .evolucionTexto(ec.getEvolucionTexto())
                .fechaActualizacion(ec.getFechaActualizacion())
                .build();
    }
}
