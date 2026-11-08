package com.consultorio.agendausuarios.dto.response;

import com.consultorio.agendausuarios.domain.enums.RolUsuario;

public record UsuarioResponseDTO(
        Long id,
        String dni,
        String nombre,
        String apellido,
        String email,
        RolUsuario rol,
        String matricula,
        Boolean activo
        ) {}
