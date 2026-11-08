package com.consultorio.atencionclinica.repository;

import com.consultorio.atencionclinica.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByDni(String dni);

    boolean existsByDni(String dni);


    List<Paciente> findByActivoTrue();

    List<Paciente> findByApellidoContainingIgnoreCaseOrNombreContainingIgnoreCase(
            String apellido, String nombre);

    List<Paciente> findByApellidoContainingIgnoreCaseAndNombreContainingIgnoreCase(
            String apellido, String nombre);
}
