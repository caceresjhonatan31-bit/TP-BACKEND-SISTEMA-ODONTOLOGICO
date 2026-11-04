package com.consultorio.atencionclinica.repository;

import com.consultorio.atencionclinica.model.EvolucionClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para {@link EvolucionClinica}.
 *
 * RN-15: Notas evolutivas pueden ser de sesión única o de rango de turnos.
 * RN-16: Acceso restringido a ADMIN o al ODONTÓLOGO que atendió al paciente.
 *         La verificación de acceso se realiza en la capa de servicio consultando
 *         este repositorio o TurnoPracticaRealizadaRepository.
 */
@Repository
public interface EvolucionClinicaRepository extends JpaRepository<EvolucionClinica, Long> {

    /**
     * Historial clínico completo de un paciente, ordenado cronológicamente desc.
     * Acceso controlado por RN-16 (verificado en capa de servicio).
     */
    List<EvolucionClinica> findByPacienteIdOrderByFechaActualizacionDesc(Long pacienteId);

    /**
     * Busca evoluciones de un paciente por turno de inicio.
     * Usado para verificar si ya existe una nota para un turno dado.
     */
    List<EvolucionClinica> findByPacienteIdAndTurnoDesdeId(Long pacienteId, Long turnoDesdeId);

    /**
     * Verifica si un odontólogo tiene registrada al menos una evolución clínica
     * en la historia de un paciente. Usado para RN-16.
     */
    boolean existsByPacienteIdAndActualizadoPorOdontologoId(Long pacienteId, Long odontologoId);

    /**
     * Todas las notas clínicas registradas/actualizadas por un odontólogo específico.
     * Útil para auditoría y para validar RN-16.
     */
    List<EvolucionClinica> findByActualizadoPorOdontologoId(Long odontologoId);

    /**
     * Verifica si existe alguna evolución para un turno específico.
     * Usado para evitar duplicar notas del mismo turno inicial.
     */
    @Query("SELECT COUNT(e) > 0 FROM EvolucionClinica e WHERE e.turnoDesdeId = :turnoId OR e.turnoHastaId = :turnoId")
    boolean existsByTurnoId(@Param("turnoId") Long turnoId);
}
