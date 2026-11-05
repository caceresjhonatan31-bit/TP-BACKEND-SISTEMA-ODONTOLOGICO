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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para el registro y consulta de prácticas odontológicas efectivamente realizadas.
 *
 * RN-10, RN-11: Registro de prestaciones efectivas post-atención médica.
 * RN-16: Confidencialidad en el acceso al historial de prácticas del paciente.
 */
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

    /**
     * Registra una práctica efectivamente realizada en una sesión médica.
     * RN-10 / RN-11:
     * 1. Verifica paciente activo.
     * 2. Verifica práctica vigente (activo = true).
     * 3. Verifica turno mediante AgendaUsuariosClient (existencia, correspondencia de paciente y odontólogo, estado válido).
     */
    public TurnoPracticaRealizadaResponseDTO registrarPracticaRealizada(TurnoPracticaRealizadaRequestDTO dto) {
        // 1. Validar Paciente
        Paciente paciente = pacienteRepository.findById(dto.getPacienteId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ningún paciente con ID: " + dto.getPacienteId()));

        if (!paciente.getActivo()) {
            throw new ConflictException(
                    "El paciente con ID " + dto.getPacienteId() + " está dado de baja. No se pueden asentar prácticas a un paciente inactivo.");
        }

        // 2. Validar Práctica vigente (RN-10)
        PracticaOdontologica practica = practicaRepository.findById(dto.getPracticaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ninguna práctica odontológica con ID: " + dto.getPracticaId()));

        if (!practica.getActivo()) {
            throw new ConflictException(
                    "La práctica [" + practica.getCodigoNomenclador() + "] '" + practica.getNombre() +
                    "' está inhabilitada (RN-10). Solo se pueden asentar prestaciones vigentes.");
        }

        // 3. Validar Turno en ms-agenda-usuarios
        TurnoExternalDTO turno = agendaUsuariosClient.obtenerTurno(dto.getTurnoId());

        if (!turno.getPacienteId().equals(dto.getPacienteId())) {
            throw new ConflictException(
                    "Inconsistencia: El turno ID " + dto.getTurnoId() + " pertenece al paciente ID " +
                    turno.getPacienteId() + ", pero se intentó registrar con paciente ID " + dto.getPacienteId());
        }

        if (!turno.getOdontologoId().equals(dto.getOdontologoId())) {
            throw new ConflictException(
                    "Inconsistencia: El turno ID " + dto.getTurnoId() + " fue asignado al odontólogo ID " +
                    turno.getOdontologoId() + ", pero se intentó registrar con odontólogo ID " + dto.getOdontologoId());
        }

        if ("CANCELADO".equalsIgnoreCase(turno.getEstado()) || "INASISTIDO".equalsIgnoreCase(turno.getEstado())) {
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

        return toResponseDTO(turnoPracticaRealizadaRepository.save(tpr));
    }

    /**
     * Lista todas las prácticas realizadas en un turno específico.
     */
    @Transactional(readOnly = true)
    public List<TurnoPracticaRealizadaResponseDTO> listarPorTurno(Long turnoId) {
        return turnoPracticaRealizadaRepository.findByTurnoId(turnoId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Lista las prácticas realizadas a un paciente.
     * RN-16: Valida confidencialidad médica (solo ADMIN o el odontólogo que atendió al paciente).
     */
    @Transactional(readOnly = true)
    public List<TurnoPracticaRealizadaResponseDTO> listarPorPaciente(Long pacienteId, Long odontologoIdSolicitante, boolean esAdmin) {
        if (!pacienteRepository.existsById(pacienteId)) {
            throw new ResourceNotFoundException("No se encontró ningún paciente con ID: " + pacienteId);
        }

        if (!esAdmin) {
            boolean atendio = turnoPracticaRealizadaRepository.existsByPacienteIdAndOdontologoId(pacienteId, odontologoIdSolicitante)
                    || evolucionClinicaRepository.existsByPacienteIdAndActualizadoPorOdontologoId(pacienteId, odontologoIdSolicitante);
            if (!atendio) {
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
