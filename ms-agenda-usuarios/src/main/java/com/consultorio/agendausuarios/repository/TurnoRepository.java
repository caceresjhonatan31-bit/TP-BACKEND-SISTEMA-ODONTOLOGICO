package com.consultorio.agendausuarios.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.consultorio.agendausuarios.domain.entity.Turno;
import com.consultorio.agendausuarios.domain.enums.EstadoTurno;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    // RN-05: Verifica si el odontólogo ya tiene un turno no cancelado superpuesto en esa fecha
    @Query("""
        SELECT COUNT(t) > 0 FROM Turno t
        WHERE t.odontologo.id = :odontologoId
          AND t.fecha = :fecha
          AND t.estado <> com.consultorio.agendausuarios.domain.enums.EstadoTurno.CANCELADO
          AND t.horaInicio < :horaFin
          AND t.horaFin > :horaInicio
    """)
    boolean existeSuperposicionOdontologo(
            @Param("odontologoId") Long odontologoId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin
    );

    // RN-05: Verifica si el paciente ya tiene un turno no cancelado superpuesto en esa fecha
    @Query("""
        SELECT COUNT(t) > 0 FROM Turno t
        WHERE t.pacienteId = :pacienteId
          AND t.fecha = :fecha
          AND t.estado <> com.consultorio.agendausuarios.domain.enums.EstadoTurno.CANCELADO
          AND t.horaInicio < :horaFin
          AND t.horaFin > :horaInicio
    """)
    boolean existeSuperposicionPaciente(
            @Param("pacienteId") Long pacienteId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin
    );

    // RN-08: Obtiene turnos PROGRAMADOS cuyo horario de finalización ya venció
    @Query("""
        SELECT t FROM Turno t
        WHERE t.estado = com.consultorio.agendausuarios.domain.enums.EstadoTurno.PROGRAMADO
          AND (t.fecha < :fechaActual OR (t.fecha = :fechaActual AND t.horaFin <= :horaActual))
    """)
    List<Turno> findTurnosProgramadosVencidos(
            @Param("fechaActual") LocalDate fechaActual,
            @Param("horaActual") LocalTime horaActual
    );

    // Consulta dinámica por filtros opcionales
    @Query("""
        SELECT t FROM Turno t
        WHERE (:odontologoId IS NULL OR t.odontologo.id = :odontologoId)
          AND (:pacienteId IS NULL OR t.pacienteId = :pacienteId)
          AND (:fecha IS NULL OR t.fecha = :fecha)
          AND (:estado IS NULL OR t.estado = :estado)
        ORDER BY t.fecha ASC, t.horaInicio ASC
    """)
    List<Turno> buscarConFiltros(
            @Param("odontologoId") Long odontologoId,
            @Param("pacienteId") Long pacienteId,
            @Param("fecha") LocalDate fecha,
            @Param("estado") EstadoTurno estado
    );
}
