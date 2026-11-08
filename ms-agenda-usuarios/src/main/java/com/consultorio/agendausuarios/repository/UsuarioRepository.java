package com.consultorio.agendausuarios.repository;

import com.consultorio.agendausuarios.domain.entity.Usuario;
import com.consultorio.agendausuarios.domain.enums.RolUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByDni(String dni);

    boolean existsByEmail(String email);

    boolean existsByMatricula(String matricula);

    boolean existsByDniAndIdNot(String dni, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByMatriculaAndIdNot(String matricula, Long id);

    Optional<Usuario> findByKeycloakId(String keycloakId);

    List<Usuario> findByActivoTrue();

    List<Usuario> findByRolAndActivoTrue(RolUsuario rol);
}
