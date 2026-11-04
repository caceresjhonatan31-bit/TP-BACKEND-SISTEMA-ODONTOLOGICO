package com.consultorio.atencionclinica.repository;

import com.consultorio.atencionclinica.model.TurnoPracticaRealizada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para {@link TurnoPracticaRealizada}.
 *
 * RN-10, RN-11: Las prácticas registradas aquí deben haber estado activas al momento del acto.
 * RN-16: existsByPacienteIdAndOdontologoId → determina si el odontólogo atendió al paciente
 *         (prerequisito para acceder a su historia clínica confidencial).
 */
@Repository
public interface TurnoPracticaRealizadaRepository extends JpaRepository<TurnoPracticaRealizada, Long> {

    /**
     * Lista todas las prácticas realizadas en un turno específico.
     * Usado por ms-agenda-usuarios vía REST para consultar el detalle de un turno atendido.
     */
    List<TurnoPracticaRealizada> findByTurnoId(Long turnoId);

    /**
     * Historial de prácticas realizadas sobre un paciente (en todos sus turnos).
     * Acceso controlado por RN-16.
     */
    List<TurnoPracticaRealizada> findByPacienteId(Long pacienteId);

    /**
     * Prácticas registradas por un odontólogo (para auditoría y control de acceso RN-16).
     */
    List<TurnoPracticaRealizada> findByOdontologoId(Long odontologoId);

    /**
     * Prácticas realizadas a un paciente por un odontólogo específico.
     */
    List<TurnoPracticaRealizada> findByPacienteIdAndOdontologoId(Long pacienteId, Long odontologoId);

    /**
     * RN-16: Verifica si el odontólogo atendió al paciente al menos una vez.
     * Usado en la capa de servicio para autorizar el acceso a la historia clínica confidencial.
     */
    boolean existsByPacienteIdAndOdontologoId(Long pacienteId, Long odontologoId);

    /**
     * Todas las prácticas realizadas en un turno para un paciente dado.
     * Verificación cruzada de integridad (el turno debe pertenecer al paciente).
     */
    List<TurnoPracticaRealizada> findByTurnoIdAndPacienteId(Long turnoId, Long pacienteId);
}
