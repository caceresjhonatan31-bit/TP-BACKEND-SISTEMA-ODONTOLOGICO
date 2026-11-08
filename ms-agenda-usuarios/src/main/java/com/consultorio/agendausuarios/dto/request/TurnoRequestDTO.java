package com.consultorio.agendausuarios.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record TurnoRequestDTO(
        @NotNull(message = "El ID del odontólogo es obligatorio")
        Long odontologoId,
        @NotNull(message = "El ID del paciente es obligatorio")
        Long pacienteId,
        @NotNull(message = "El ID de la práctica inicial es obligatorio")
        Long practicaInicialId,
        @NotNull(message = "La fecha del turno es obligatoria")
        LocalDate fecha,
        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,
        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,
        String notaAclaratoria
        ) {}
