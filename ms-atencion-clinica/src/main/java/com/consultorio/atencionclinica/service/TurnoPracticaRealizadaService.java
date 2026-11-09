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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class TurnoPracticaRealizadaService {

    private final TurnoPracticaRealizadaRepository turnoPracticaRealizadaRepository;
    private final PacienteRepository pacienteRepository;
    private final PracticaOdontologicaRepository practicaRepository;
    private final EvolucionClinicaRepository evolucionClinicaRepository;
    private final AgendaUsuariosClient agendaUsuariosClient;

    public TurnoPracticaRealizadaService(TurnoPracticaRealizadaRepository turnoPracticaRealizadaRepository,
                                         PacienteRepository pacienteRepository,
                                         PracticaOdontologicaRepository practicaRepository,
                                         EvolucionClinicaRepository evolucionClinicaRepository,
                                         AgendaUsuariosClient agendaUsuariosClient) {
        this.turnoPracticaRealizadaRepository = turnoPracticaRealizadaRepository;
        this.pacienteRepository = pacienteRepository;
        this.practicaRepository = practicaRepository;
        this.evolucionClinicaRepository = evolucionClinicaRepository;
        this.agendaUsuariosClient = agendaUsuariosClient;
    }

    public TurnoPracticaRealizadaResponseDTO registrarPracticaRealizada(TurnoPracticaRealizadaRequestDTO dto) {
        log.debug("Iniciando validaciones RN-10 y RN-11 para asentar práctica ID {} en turno ID {} (paciente ID: {})",
                dto.getPracticaId(), dto.getTurnoId(), dto.getPacienteId());

        // 1. Validar Paciente
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> {
                    log.warn("Rechazo de práctica efectuada: Paciente ID {} no encontrado", dto.getPacienteId());
                    return new ResourceNotFoundException("No se encontró ningún paciente con ID: " + dto.getPacienteId());
                });

        if (!paciente.getActivo()) {
            log.warn("Rechazo de práctica efectuada: Paciente ID {} está dado de baja (inactivo)", dto.getPacienteId());
            throw new ConflictException(
                    "El paciente con ID " + dto.getPacienteId() + " está dado de baja. No se pueden asentar prácticas a un paciente inactivo.");
        }

        // 2. Validar Práctica vigente (RN-10)
        PracticaOdontologica practica = practicaRepository.findById(dto.getPracticaId())
                .orElseThrow(() -> {
                    log.warn("Rechazo de práctica efectuada (RN-10): Práctica ID {} no encontrada", dto.getPracticaId());
                    return new ResourceNotFoundException("No se encontró ninguna práctica odontológica con ID: " + dto.getPracticaId());
                });

        if (!practica.getActivo()) {
            log.warn("Rechazo de práctica efectuada (RN-10): Práctica ID {} inhabilitada", dto.getPracticaId());
            throw new ConflictException(
                    "La práctica [" + practica.getCodigoNomenclador() + "] '" + practica.getNombre() +
                    "' está inhabilitada (RN-10). Solo se pueden asentar prestaciones vigentes.");
        }

        // 3. Validar Turno en ms-agenda-usuarios
        TurnoExternalDTO turno = agendaUsuariosClient.obtenerTurno(dto.getTurnoId());

        if (!turno.getPacienteId().equals(dto.getPacienteId())) {
            log.warn("Rechazo de práctica efectuada (RN-11): Inconsistencia de paciente (turno pertenece a paciente ID {}, recibido ID {})",
                    turno.getPacienteId(), dto.getPacienteId());
            throw new ConflictException(
                    "Inconsistencia: El turno ID " + dto.getTurnoId() + " pertenece al paciente ID " +
                    turno.getPacienteId() + ", pero se intentó registrar con paciente ID " + dto.getPacienteId());
        }

        if (!turno.getOdontologoId().equals(dto.getOdontologoId())) {
            log.warn("Rechazo de práctica efectuada (RN-11): Inconsistencia de odontólogo (turno asignado a odontólogo ID {}, recibido ID {})",
                    turno.getOdontologoId(), dto.getOdontologoId());
            throw new ConflictException(
                    "Inconsistencia: El turno ID " + dto.getTurnoId() + " fue asignado al odontólogo ID " +
                    turno.getOdontologoId() + ", pero se intentó registrar con odontólogo ID " + dto.getOdontologoId());
        }

        if ("CANCELADO".equalsIgnoreCase(turno.getEstado()) || "INASISTIDO".equalsIgnoreCase(turno.getEstado())) {
            log.warn("Rechazo de práctica efectuada (RN-11): Turno ID {} en estado incompatible '{}'",
                    turno.getId(), turno.getEstado());
            throw new ConflictException(
                    "No se pueden registrar prácticas sobre un turno en estado '" + turno.getEstado() +
                    "'. El turno debe encontrarse en estado PROGRAMADO o ATENDIDO.");
        }

        // 4. Persistir registro de práctica realizada
        TurnoPracticaRealizada tpr = new TurnoPracticaRealizada();
        tpr.setTurnoId(dto.getTurnoId());
        tpr.setPaciente(paciente);
        tpr.setPractica(practica);
        tpr.setOdontologoId(dto.getOdontologoId());
        tpr.setNotaAclaratoria(dto.getNotaAclaratoria());

        TurnoPracticaRealizada guardada = turnoPracticaRealizadaRepository.save(tpr);
        log.info("Práctica efectuada registrada exitosamente con ID: {} (turnoId: {}, practicaId: {}, pacienteId: {})",
                guardada.getId(), guardada.getTurnoId(), guardada.getPractica().getId(), guardada.getPaciente().getId());

        return toResponseDTO(guardada);
    }

    @Transactional(readOnly = true)
    public List<TurnoPracticaRealizadaResponseDTO> listarPorTurno(Long turnoId) {
        log.debug("Consultando prácticas efectuadas en turno ID: {}", turnoId);
        return turnoPracticaRealizadaRepository.findByTurnoId(turnoId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TurnoPracticaRealizadaResponseDTO> listarPorPaciente(Long pacienteId, Long odontologoIdSolicitante, boolean esAdmin) {
        log.debug("Verificando autorización RN-16 para consultar prácticas de paciente ID {} (odontologoId: {}, esAdmin: {})",
                pacienteId, odontologoIdSolicitante, esAdmin);

        if (!pacienteRepository.existsById(pacienteId)) {
            log.warn("Paciente ID {} no encontrado para consultar prácticas realizadas", pacienteId);
            throw new ResourceNotFoundException("No se encontró ningún paciente con ID: " + pacienteId);
        }

        if (!esAdmin) {
            boolean atendio = turnoPracticaRealizadaRepository.existsByPacienteIdAndOdontologoId(pacienteId, odontologoIdSolicitante)
                    || evolucionClinicaRepository.existsByPacienteIdAndActualizadoPorOdontologoId(pacienteId, odontologoIdSolicitante);
            if (!atendio) {
                log.warn("Rechazo de acceso a historia clínica (RN-16): Odontólogo ID {} no registra atenciones previas con paciente ID {}",
                        odontologoIdSolicitante, pacienteId);
                throw new ConflictException(
                        "Acceso denegado: El odontólogo ID " + odontologoIdSolicitante +
                        " no ha atendido al paciente ID " + pacienteId +
                        " y no tiene acceso a sus prácticas realizadas (RN-16).");
            }
        }

        return turnoPracticaRealizadaRepository.findByPacienteId(pacienteId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TurnoPracticaRealizadaResponseDTO toResponseDTO(TurnoPracticaRealizada tpr) {
        return TurnoPracticaRealizadaResponseDTO.builder()
                .id(tpr.getId())
                .turnoId(tpr.getTurnoId())
                .pacienteId(tpr.getPaciente().getId())
                .practicaId(tpr.getPractica().getId())
                .codigoNomenclador(tpr.getPractica().getCodigoNomenclador())
                .nombrePractica(tpr.getPractica().getNombre())
                .odontologoId(tpr.getOdontologoId())
                .notaAclaratoria(tpr.getNotaAclaratoria())
                .fechaRegistro(tpr.getFechaRegistro())
                .build();
    }
}
