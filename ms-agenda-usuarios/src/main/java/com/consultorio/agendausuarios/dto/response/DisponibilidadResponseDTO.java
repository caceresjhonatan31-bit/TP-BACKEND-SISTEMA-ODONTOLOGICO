package com.consultorio.agendausuarios.dto.response;

import java.time.LocalTime;

import com.consultorio.agendausuarios.domain.enums.DiaSemana;

public record DisponibilidadResponseDTO(
        Long id,
        Long odontologoId,
        DiaSemana diaSemana,
        LocalTime horaInicio,
        LocalTime horaFin,
        Boolean activo
        ) {}
