package com.consultorio.agendausuarios.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

import com.consultorio.agendausuarios.domain.enums.EstadoTurno;

public record TurnoResponseDTO(
        Long id,
        Long odontologoId,
        String odontologoNombreCompleto,
        Long pacienteId,
        Long practicaInicialId,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        EstadoTurno estado,
        String notaAclaratoria,
        String googleEventId
        ) {}
