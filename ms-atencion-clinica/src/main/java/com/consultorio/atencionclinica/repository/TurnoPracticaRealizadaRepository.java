package com.consultorio.atencionclinica.repository;

import com.consultorio.atencionclinica.model.TurnoPracticaRealizada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TurnoPracticaRealizadaRepository extends JpaRepository<TurnoPracticaRealizada, Long> {

    List<TurnoPracticaRealizada> findByTurnoId(Long turnoId);

  
    List<TurnoPracticaRealizada> findByPacienteId(Long pacienteId);

    List<TurnoPracticaRealizada> findByOdontologoId(Long odontologoId);

    List<TurnoPracticaRealizada> findByPacienteIdAndOdontologoId(Long pacienteId, Long odontologoId);

    boolean existsByPacienteIdAndOdontologoId(Long pacienteId, Long odontologoId);

    List<TurnoPracticaRealizada> findByTurnoIdAndPacienteId(Long turnoId, Long pacienteId);
}
