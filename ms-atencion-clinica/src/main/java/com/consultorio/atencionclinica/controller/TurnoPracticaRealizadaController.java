package com.consultorio.atencionclinica.controller;

import com.consultorio.atencionclinica.dto.request.TurnoPracticaRealizadaRequestDTO;
import com.consultorio.atencionclinica.dto.response.TurnoPracticaRealizadaResponseDTO;
import com.consultorio.atencionclinica.service.TurnoPracticaRealizadaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para el registro y consulta de prácticas odontológicas efectivamente realizadas en turnos.
 *
 * Mapeo: /api/practicas-realizadas
 * RN-10, RN-11: Registro de prestaciones post-atención.
 * RN-16: Confidencialidad de los datos médicos.
 */
@RestController
@RequestMapping("/api/practicas-realizadas")
@RequiredArgsConstructor
public class TurnoPracticaRealizadaController {

    private final TurnoPracticaRealizadaService turnoPracticaRealizadaService;

    @PostMapping
    public ResponseEntity<TurnoPracticaRealizadaResponseDTO> registrarPracticaRealizada(
            @Valid @RequestBody TurnoPracticaRealizadaRequestDTO dto) {
        TurnoPracticaRealizadaResponseDTO realizada = turnoPracticaRealizadaService.registrarPracticaRealizada(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(realizada);
    }

    @GetMapping("/turno/{turnoId}")
    public ResponseEntity<List<TurnoPracticaRealizadaResponseDTO>> listarPorTurno(@PathVariable Long turnoId) {
        return ResponseEntity.ok(turnoPracticaRealizadaService.listarPorTurno(turnoId));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<TurnoPracticaRealizadaResponseDTO>> listarPorPaciente(
            @PathVariable Long pacienteId,
            @RequestParam Long odontologoId,
            @RequestParam(defaultValue = "false") boolean esAdmin) {
        return ResponseEntity.ok(turnoPracticaRealizadaService.listarPorPaciente(pacienteId, odontologoId, esAdmin));
    }
}
