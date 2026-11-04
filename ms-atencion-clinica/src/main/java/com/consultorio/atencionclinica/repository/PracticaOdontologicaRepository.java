package com.consultorio.atencionclinica.repository;

import com.consultorio.atencionclinica.model.PracticaOdontologica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link PracticaOdontologica}.
 *
 * RN-10, RN-11: Solo se pueden registrar en actos médicos las prácticas con activo = true.
 */
@Repository
public interface PracticaOdontologicaRepository extends JpaRepository<PracticaOdontologica, Long> {

    /**
     * Busca por código de nomenclador único.
     * Usado para: (a) detectar duplicados al crear, (b) consulta por código.
     */
    Optional<PracticaOdontologica> findByCodigoNomenclador(String codigoNomenclador);

    /**
     * Verifica existencia de código para prevenir duplicados (lanza ConflictException si existe).
     */
    boolean existsByCodigoNomenclador(String codigoNomenclador);

    /**
     * Lista solo las prácticas vigentes (activo = true).
     * Usada por ms-agenda-usuarios al consultar prácticas disponibles para asignar en un turno.
     */
    List<PracticaOdontologica> findByActivoTrue();

    /**
     * Busca una práctica activa por ID.
     * Usada en: validación inter-servicio GET /api/practicas/{id}/validar-activa
     * y en registro de prácticas realizadas (RN-10).
     */
    Optional<PracticaOdontologica> findByIdAndActivoTrue(Long id);
}
