package com.consultorio.agendausuarios.repository;

import com.consultorio.agendausuarios.domain.entity.DisponibilidadOdontologo;
import com.consultorio.agendausuarios.domain.enums.DiaSemana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface DisponibilidadRepository extends JpaRepository<DisponibilidadOdontologo, Long> {

    List<DisponibilidadOdontologo> findByOdontologoIdAndActivoTrue(Long odontologoId);

    // RN-02: Detecta si una nueva franja se solapa con otra activa del mismo odontólogo y día
    @Query("""
        SELECT COUNT(d) > 0 FROM DisponibilidadOdontologo d
        WHERE d.odontologo.id = :odontologoId
          AND d.diaSemana = :diaSemana
          AND d.activo = true
          AND d.horaInicio < :horaFin
          AND d.horaFin > :horaInicio
    """)
    boolean existeSuperposicionHoraria(
            @Param("odontologoId") Long odontologoId,
            @Param("diaSemana") DiaSemana diaSemana,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin
    );

    // RN-02 (en actualización PUT): Detecta solapamiento excluyendo la franja que se está editando
    @Query("""
        SELECT COUNT(d) > 0 FROM DisponibilidadOdontologo d
        WHERE d.odontologo.id = :odontologoId
          AND d.diaSemana = :diaSemana
          AND d.activo = true
          AND d.id <> :disponibilidadId
          AND d.horaInicio < :horaFin
          AND d.horaFin > :horaInicio
    """)
    boolean existeSuperposicionHorariaExcluyendoId(
            @Param("odontologoId") Long odontologoId,
            @Param("diaSemana") DiaSemana diaSemana,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin,
            @Param("disponibilidadId") Long disponibilidadId
    );

    // RN-03: Verifica si el rango del turno entra dentro de una franja activa del odontólogo
    @Query("""
        SELECT COUNT(d) > 0 FROM DisponibilidadOdontologo d
        WHERE d.odontologo.id = :odontologoId
          AND d.diaSemana = :diaSemana
          AND d.activo = true
          AND d.horaInicio <= :horaInicioTurno
          AND d.horaFin >= :horaFinTurno
    """)
    boolean turnoDentroDeFranjaProfesional(
            @Param("odontologoId") Long odontologoId,
            @Param("diaSemana") DiaSemana diaSemana,
            @Param("horaInicioTurno") LocalTime horaInicioTurno,
            @Param("horaFinTurno") LocalTime horaFinTurno
    );
}
