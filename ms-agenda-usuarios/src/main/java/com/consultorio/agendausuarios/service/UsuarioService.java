package com.consultorio.agendausuarios.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.consultorio.agendausuarios.domain.entity.Usuario;
import com.consultorio.agendausuarios.domain.enums.RolUsuario;
import com.consultorio.agendausuarios.dto.request.UsuarioRequestDTO;
import com.consultorio.agendausuarios.dto.response.UsuarioResponseDTO;
import com.consultorio.agendausuarios.repository.UsuarioRepository;
import com.consultorio.common.exception.ConflictException;
import com.consultorio.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public UsuarioResponseDTO crearUsuario(UsuarioRequestDTO request) {
        if (usuarioRepository.existsByDni(request.dni())) {
            throw new ConflictException("Ya existe un usuario registrado con el DNI: " + request.dni());
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictException("Ya existe un usuario registrado con el email: " + request.email());
        }
        validarMatriculaOdontologo(request.rol(), request.matricula(), null);

        Usuario usuario = Usuario.builder()
                .dni(request.dni())
                .nombre(request.nombre())
                .apellido(request.apellido())
                .email(request.email())
                .rol(request.rol())
                .matricula(request.matricula())
                .activo(true)
                .build();

        return mapToResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarUsuarios(Boolean soloActivos) {
        List<Usuario> usuarios = Boolean.TRUE.equals(soloActivos)
                ? usuarioRepository.findByActivoTrue()
                : usuarioRepository.findAll();
        return usuarios.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerPorId(Long id) {
        Usuario usuario = buscarEntidadPorId(id);
        return mapToResponse(usuario);
    }

    @Transactional
    public UsuarioResponseDTO actualizarUsuario(Long id, UsuarioRequestDTO request) {
        Usuario usuario = buscarEntidadPorId(id);

        if (usuarioRepository.existsByDniAndIdNot(request.dni(), id)) {
            throw new ConflictException("El DNI ya pertenece a otro usuario del consultorio");
        }
        if (usuarioRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ConflictException("El email ya pertenece a otro usuario del consultorio");
        }
        validarMatriculaOdontologo(request.rol(), request.matricula(), id);

        usuario.setDni(request.dni());
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email());
        usuario.setRol(request.rol());
        usuario.setMatricula(request.matricula());

        return mapToResponse(usuarioRepository.save(usuario));
    }

    @Transactional
    // RN-01: Preservación del historial laboral (pasa a inactivo sin borrarse físicamente)
    public void darDeBaja(Long id) {
        Usuario usuario = buscarEntidadPorId(id);
        if (!usuario.getActivo()) {
            throw new ConflictException("El usuario ya se encuentra inactivo");
        }
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    private Usuario buscarEntidadPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    private void validarMatriculaOdontologo(RolUsuario rol, String matricula, Long idActual) {
        if (rol == RolUsuario.ODONTOLOGO && (matricula == null || matricula.isBlank())) {
            throw new ConflictException("La matrícula profesional es obligatoria para el rol ODONTOLOGO");
        }
        if (matricula != null && !matricula.isBlank()) {
            boolean existe = (idActual == null)
                    ? usuarioRepository.existsByMatricula(matricula)
                    : usuarioRepository.existsByMatriculaAndIdNot(matricula, idActual);
            if (existe) {
                throw new ConflictException("La matrícula ya se encuentra registrada en otro profesional");
            }
        }
    }

    private UsuarioResponseDTO mapToResponse(Usuario u) {
        return new UsuarioResponseDTO(
                u.getId(),
                u.getDni(),
                u.getNombre(),
                u.getApellido(),
                u.getEmail(),
                u.getRol(),
                u.getMatricula(),
                u.getActivo()
        );
    }
}
