package com.consultorio.agendausuarios.dto.request;

import com.consultorio.agendausuarios.domain.enums.RolUsuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsuarioRequestDTO(
        @NotBlank(message = "El DNI es obligatorio")
        String dni,
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,
        @NotBlank(message = "El apellido es obligatorio")
        String apellido,
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email es inválido")
        String email,
        @NotNull(message = "El rol es obligatorio")
        RolUsuario rol,
        String matricula
        ) {}
