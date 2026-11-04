package com.consultorio.atencionclinica.repository;

import com.consultorio.atencionclinica.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link Paciente}.
 *
 * RN-13: Búsqueda por DNI para detectar si el paciente ya existe (activo o inactivo)
 *         antes de registrar uno nuevo o reactivar el existente.
 */
@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    /**
     * Busca un paciente por DNI independientemente de su estado activo/inactivo.
     * Usado para: (a) detectar DNI duplicado al crear, (b) reactivar paciente de baja (RN-13).
     */
    Optional<Paciente> findByDni(String dni);

    /**
     * Verifica si existe algún paciente con ese DNI (activo o inactivo).
     * Permite lanzar ConflictException al intentar registrar un DNI ya existente.
     */
    boolean existsByDni(String dni);

    /**
     * Lista todos los pacientes activos.
     * Usado por recepción para visualizar fichas vigentes.
     */
    List<Paciente> findByActivoTrue();

    /**
     * Búsqueda flexible por apellido o nombre (case-insensitive, parcial).
     * Requerida por F-01 / RN-13: "Busca ficha por DNI o por nombre y reactiva si está de baja".
     */
    List<Paciente> findByApellidoContainingIgnoreCaseOrNombreContainingIgnoreCase(
            String apellido, String nombre);

    /**
     * Búsqueda por apellido y nombre exactos (para búsqueda combinada).
     */
    List<Paciente> findByApellidoContainingIgnoreCaseAndNombreContainingIgnoreCase(
            String apellido, String nombre);
}
