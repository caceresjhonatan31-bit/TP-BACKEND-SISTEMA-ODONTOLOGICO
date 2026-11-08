package com.consultorio.agendausuarios.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.consultorio.agendausuarios.dto.request.UsuarioRequestDTO;
import com.consultorio.agendausuarios.dto.response.UsuarioResponseDTO;
import com.consultorio.agendausuarios.service.UsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO crearUsuario(@Valid @RequestBody UsuarioRequestDTO request) {
        return usuarioService.crearUsuario(request);
    }

    @GetMapping
    public List<UsuarioResponseDTO> listarUsuarios(
            @RequestParam(defaultValue = "true") Boolean soloActivos) {
        return usuarioService.listarUsuarios(soloActivos);
    }

    @GetMapping("/{id}")
    public UsuarioResponseDTO obtenerPorId(@PathVariable Long id) {
        return usuarioService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public UsuarioResponseDTO actualizarUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioRequestDTO request) {
        return usuarioService.actualizarUsuario(id, request);
    }

    @PatchMapping("/{id}/baja")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void darDeBaja(@PathVariable Long id) {
        usuarioService.darDeBaja(id);
    }
}
