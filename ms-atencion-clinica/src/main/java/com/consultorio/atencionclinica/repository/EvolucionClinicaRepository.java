package com.consultorio.atencionclinica.repository;

import com.consultorio.atencionclinica.model.EvolucionClinica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvolucionClinicaRepository extends JpaRepository<EvolucionClinica, Long> {

    List<EvolucionClinica> findByPacienteIdOrderByFechaActualizacionDesc(Long pacienteId);

    List<EvolucionClinica> findByPacienteIdAndTurnoDesdeId(Long pacienteId, Long turnoDesdeId);


    boolean existsByPacienteIdAndActualizadoPorOdontologoId(Long pacienteId, Long odontologoId);

    List<EvolucionClinica> findByActualizadoPorOdontologoId(Long odontologoId);

    @Query("SELECT COUNT(e) > 0 FROM EvolucionClinica e WHERE e.turnoDesdeId = :turnoId OR e.turnoHastaId = :turnoId")
    boolean existsByTurnoId(@Param("turnoId") Long turnoId);
}
