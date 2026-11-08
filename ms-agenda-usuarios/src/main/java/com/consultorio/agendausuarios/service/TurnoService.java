package com.consultorio.agendausuarios.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.consultorio.agendausuarios.client.AtencionClinicaClient;
import com.consultorio.agendausuarios.client.GoogleCalendarClient;
import com.consultorio.agendausuarios.domain.entity.Turno;
import com.consultorio.agendausuarios.domain.entity.Usuario;
import com.consultorio.agendausuarios.domain.enums.DiaSemana;
import com.consultorio.agendausuarios.domain.enums.EstadoTurno;
import com.consultorio.agendausuarios.domain.enums.RolUsuario;
import com.consultorio.agendausuarios.dto.request.TurnoRequestDTO;
import com.consultorio.agendausuarios.dto.response.FeriadoResponseDTO;
import com.consultorio.agendausuarios.dto.response.TurnoResponseDTO;
import com.consultorio.agendausuarios.repository.DisponibilidadRepository;
import com.consultorio.agendausuarios.repository.TurnoRepository;
import com.consultorio.agendausuarios.repository.UsuarioRepository;
import com.consultorio.common.exception.ConflictException;
import com.consultorio.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TurnoService {

    private static final int MINUTOS_TOLERANCIA = 30;

    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadRepository disponibilidadRepository;
    private final GoogleCalendarClient googleCalendarClient;
    private final AtencionClinicaClient atencionClinicaClient;

    @Transactional
    public TurnoResponseDTO otorgarTurno(TurnoRequestDTO request) {
        if (!request.horaInicio().isBefore(request.horaFin())) {
            throw new ConflictException("La hora de inicio del turno debe ser anterior a la hora de finalización");
        }

        // RN-01: Verificar que el profesional exista, sea ODONTOLOGO y se encuentre activo
        Usuario odontologo = usuarioRepository.findById(request.odontologoId())
                .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado con ID: " + request.odontologoId()));

        if (!odontologo.getActivo() || odontologo.getRol() != RolUsuario.ODONTOLOGO) {
            throw new ConflictException("No se pueden asignar turnos a personal inactivo o que no sea odontólogo (RN-01)");
        }

        // RN-04: Calendario de ocupación de los odontólogos - Validar feriados de Argentina en Google Calendar / API externa
        googleCalendarClient.validarDiaNoFeriado(request.fecha());

        // RN-03: Respeto de la agenda profesional (día de la semana y franja horaria activa)
        DiaSemana diaTurno = convertirDiaSemana(request.fecha().getDayOfWeek());
        boolean dentroDeAgenda = disponibilidadRepository.turnoDentroDeFranjaProfesional(
                odontologo.getId(), diaTurno, request.horaInicio(), request.horaFin());

        if (!dentroDeAgenda) {
            throw new ConflictException("El horario solicitado está fuera de las franjas de atención del odontólogo (RN-03)");
        }

        // RN-05: Exclusividad horaria de atención para el odontólogo
        if (turnoRepository.existeSuperposicionOdontologo(
                odontologo.getId(), request.fecha(), request.horaInicio(), request.horaFin())) {
            throw new ConflictException("El odontólogo ya posee un turno asignado en ese rango horario (RN-05)");
        }

        // RN-05: Exclusividad horaria de atención para el paciente
        if (turnoRepository.existeSuperposicionPaciente(
                request.pacienteId(), request.fecha(), request.horaInicio(), request.horaFin())) {
            throw new ConflictException("El paciente ya posee otro turno simultáneo en ese rango horario (RN-05)");
        }

        // RN-06: Vigencia de paciente y prestación en ms-atencion-clinica
        atencionClinicaClient.validarVigenciaPacienteYPractica(request.pacienteId(), request.practicaInicialId());

        // RN-04 y F-04: Registrar el evento en el Google Calendar del profesional
        String googleEventId = googleCalendarClient.registrarEventoTurno(
                odontologo,
                request.pacienteId(),
                request.practicaInicialId(),
                request.fecha(),
                request.horaInicio(),
                request.horaFin(),
                request.notaAclaratoria()
        );

        Turno nuevoTurno = Turno.builder()
                .odontologo(odontologo)
                .pacienteId(request.pacienteId())
                .practicaInicialId(request.practicaInicialId())
                .fecha(request.fecha())
                .horaInicio(request.horaInicio())
                .horaFin(request.horaFin())
                .estado(EstadoTurno.PROGRAMADO)
                .notaAclaratoria(request.notaAclaratoria())
                .googleEventId(googleEventId)
                .build();

        return mapToResponse(turnoRepository.save(nuevoTurno));
    }

    @Transactional(readOnly = true)
    public List<TurnoResponseDTO> consultarTurnos(Long odontologoId, Long pacienteId, LocalDate fecha, EstadoTurno estado) {
        return turnoRepository.buscarConFiltros(odontologoId, pacienteId, fecha, estado)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TurnoResponseDTO obtenerPorId(Long id) {
        return mapToResponse(buscarEntidadPorId(id));
    }

    @Transactional
    // RN-07 y RN-12: Finalizar turno como ATENDIDO validando la tolerancia máxima de 30 minutos
    public TurnoResponseDTO marcarComoAtendido(Long id) {
        Turno turno = buscarEntidadPorId(id);

        if (turno.getEstado() != EstadoTurno.PROGRAMADO) {
            throw new ConflictException("Solo se puede marcar como atendido un turno en estado PROGRAMADO. Estado actual: " + turno.getEstado() + " (RN-12)");
        }

        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(turno.getFecha())) {
            throw new ConflictException("No se puede marcar como atendido un turno de una fecha futura");
        }

        LocalTime horaLimiteTolerancia = turno.getHoraInicio().plusMinutes(MINUTOS_TOLERANCIA);
        if (hoy.isAfter(turno.getFecha()) || LocalTime.now().isAfter(horaLimiteTolerancia)) {
            throw new ConflictException("Se superó el límite de 30 minutos de tolerancia desde el comienzo del turno; no se puede brindar la atención (RN-07)");
        }

        turno.setEstado(EstadoTurno.ATENDIDO);
        return mapToResponse(turnoRepository.save(turno));
    }

    @Transactional
    // RN-09 y RN-12: Cancelación de un turno programado estrictamente antes de su horario de inicio
    public TurnoResponseDTO cancelarTurno(Long id) {
        Turno turno = buscarEntidadPorId(id);

        if (turno.getEstado() != EstadoTurno.PROGRAMADO) {
            throw new ConflictException("El turno no admite cancelación porque ya se encuentra en estado definitivo: " + turno.getEstado() + " (RN-12)");
        }

        LocalDateTime inicioTurno = LocalDateTime.of(turno.getFecha(), turno.getHoraInicio());
        if (!LocalDateTime.now().isBefore(inicioTurno)) {
            throw new ConflictException("Solo se puede cancelar un turno antes de su horario de inicio (RN-09)");
        }

        turno.setEstado(EstadoTurno.CANCELADO);
        googleCalendarClient.eliminarEventoTurno(turno.getGoogleEventId());
        return mapToResponse(turnoRepository.save(turno));
    }

    @Transactional(readOnly = true)
    public List<FeriadoResponseDTO> consultarFeriados(Integer anio) {
        int anioConsulta = (anio != null) ? anio : LocalDate.now().getYear();
        return googleCalendarClient.obtenerFeriadosPorAnio(anioConsulta);
    }

    @Transactional
    // RN-08 y RN-12: Registro de inasistencia una vez superado el horario de finalización
    public TurnoResponseDTO marcarComoInasistido(Long id) {
        Turno turno = buscarEntidadPorId(id);

        if (turno.getEstado() != EstadoTurno.PROGRAMADO) {
            throw new ConflictException("Solo un turno PROGRAMADO puede pasar a INASISTIDO. Estado actual: " + turno.getEstado() + " (RN-12)");
        }

        LocalDateTime finTurno = LocalDateTime.of(turno.getFecha(), turno.getHoraFin());
        if (LocalDateTime.now().isBefore(finTurno)) {
            throw new ConflictException("El turno solo puede registrarse como inasistido tras superar su horario de finalización (RN-08)");
        }

        turno.setEstado(EstadoTurno.INASISTIDO);
        return mapToResponse(turnoRepository.save(turno));
    }

    @Transactional
    // RN-10 y RN-12: Excepción de rectificación de INASISTIDO a ATENDIDO dentro del mismo día
    public TurnoResponseDTO rectificarInasistencia(Long id) {
        Turno turno = buscarEntidadPorId(id);

        if (turno.getEstado() != EstadoTurno.INASISTIDO) {
            throw new ConflictException("La rectificación solo aplica a turnos en estado INASISTIDO. Estado actual: " + turno.getEstado() + " (RN-10)");
        }

        if (!turno.getFecha().isEqual(LocalDate.now())) {
            throw new ConflictException("Solo se puede rectificar un turno inasistido como atendido si corresponde al mismo día de la cita (RN-10 / RN-12)");
        }

        turno.setEstado(EstadoTurno.ATENDIDO);
        return mapToResponse(turnoRepository.save(turno));
    }

    @Transactional
    @Scheduled(fixedRate = 60000)
    // RN-08: Tarea automática que cada 60 segundos pasa a INASISTIDO los turnos vencidos
    public void procesarInasistenciasAutomaticas() {
        List<Turno> vencidos = turnoRepository.findTurnosProgramadosVencidos(LocalDate.now(), LocalTime.now());
        for (Turno turno : vencidos) {
            turno.setEstado(EstadoTurno.INASISTIDO);
        }
        if (!vencidos.isEmpty()) {
            turnoRepository.saveAll(vencidos);
        }
    }

    private Turno buscarEntidadPorId(Long id) {
        return turnoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno no encontrado con ID: " + id));
    }

    private DiaSemana convertirDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY ->
                DiaSemana.LUNES;
            case TUESDAY ->
                DiaSemana.MARTES;
            case WEDNESDAY ->
                DiaSemana.MIERCOLES;
            case THURSDAY ->
                DiaSemana.JUEVES;
            case FRIDAY ->
                DiaSemana.VIERNES;
            case SATURDAY ->
                DiaSemana.SABADO;
            case SUNDAY ->
                DiaSemana.DOMINGO;
        };
    }

    private TurnoResponseDTO mapToResponse(Turno t) {
        String nombreCompleto = t.getOdontologo().getApellido() + ", " + t.getOdontologo().getNombre();
        return new TurnoResponseDTO(
                t.getId(),
                t.getOdontologo().getId(),
                nombreCompleto,
                t.getPacienteId(),
                t.getPracticaInicialId(),
                t.getFecha(),
                t.getHoraInicio(),
                t.getHoraFin(),
                t.getEstado(),
                t.getNotaAclaratoria(),
                t.getGoogleEventId()
        );
    }
}
