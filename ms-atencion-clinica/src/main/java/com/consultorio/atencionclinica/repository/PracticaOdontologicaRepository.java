package com.consultorio.atencionclinica.repository;

import com.consultorio.atencionclinica.model.PracticaOdontologica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface PracticaOdontologicaRepository extends JpaRepository<PracticaOdontologica, Long> {

    Optional<PracticaOdontologica> findByCodigoNomenclador(String codigoNomenclador);

    boolean existsByCodigoNomenclador(String codigoNomenclador);

    List<PracticaOdontologica> findByActivoTrue();

    Optional<PracticaOdontologica> findByIdAndActivoTrue(Long id);
}
