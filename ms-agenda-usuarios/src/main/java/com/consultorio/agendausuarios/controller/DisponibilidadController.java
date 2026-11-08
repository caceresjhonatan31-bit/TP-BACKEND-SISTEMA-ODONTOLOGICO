package com.consultorio.agendausuarios.controller;

import com.consultorio.agendausuarios.dto.request.DisponibilidadRequestDTO;
import com.consultorio.agendausuarios.dto.response.DisponibilidadResponseDTO;
import com.consultorio.agendausuarios.service.DisponibilidadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disponibilidades")
@RequiredArgsConstructor
public class DisponibilidadController {

    private final DisponibilidadService disponibilidadService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DisponibilidadResponseDTO asignarFranja(@Valid @RequestBody DisponibilidadRequestDTO request) {
        return disponibilidadService.asignarFranja(request);
    }

    @GetMapping("/odontologo/{id}")
    public List<DisponibilidadResponseDTO> listarPorOdontologo(@PathVariable Long id) {
        return disponibilidadService.listarPorOdontologo(id);
    }

    @PutMapping("/{id}")
    public DisponibilidadResponseDTO actualizarFranja(
            @PathVariable Long id,
            @Valid @RequestBody DisponibilidadRequestDTO request) {
        return disponibilidadService.actualizarFranja(id, request);
    }

    @PatchMapping("/{id}/baja")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inhabilitarFranja(@PathVariable Long id) {
        disponibilidadService.inhabilitarFranja(id);
    }
}
