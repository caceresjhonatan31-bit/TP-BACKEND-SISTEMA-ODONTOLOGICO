package com.consultorio.agendausuarios.dto.request;

import java.time.LocalTime;

import com.consultorio.agendausuarios.domain.enums.DiaSemana;

import jakarta.validation.constraints.NotNull;

public record DisponibilidadRequestDTO(
        @NotNull(message = "El ID del odontólogo es obligatorio")
        Long odontologoId,
        @NotNull(message = "El día de la semana es obligatorio")
        DiaSemana diaSemana,
        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,
        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin
        ) {}
