package com.consultorio.atencionclinica.controller;

import com.consultorio.atencionclinica.dto.request.TurnoPracticaRealizadaRequestDTO;
import com.consultorio.atencionclinica.dto.response.TurnoPracticaRealizadaResponseDTO;
import com.consultorio.atencionclinica.service.TurnoPracticaRealizadaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/practicas-realizadas")
@RequiredArgsConstructor
public class TurnoPracticaRealizadaController {

    private final TurnoPracticaRealizadaService turnoPracticaRealizadaService;

    @PostMapping
    public ResponseEntity<TurnoPracticaRealizadaResponseDTO> registrarPracticaRealizada(
            @Valid @RequestBody TurnoPracticaRealizadaRequestDTO dto) {
        log.info("[POST] /api/practicas-realizadas - Registro de práctica efectuada (turnoId: {}, practicaId: {})",
                dto.getTurnoId(), dto.getPracticaId());
        TurnoPracticaRealizadaResponseDTO realizada = turnoPracticaRealizadaService.registrarPracticaRealizada(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(realizada);
    }

    @GetMapping("/turno/{turnoId}")
    public ResponseEntity<List<TurnoPracticaRealizadaResponseDTO>> listarPorTurno(@PathVariable Long turnoId) {
        log.info("[GET] /api/practicas-realizadas/turno/{} - Consulta de prácticas por turno", turnoId);
        return ResponseEntity.ok(turnoPracticaRealizadaService.listarPorTurno(turnoId));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<TurnoPracticaRealizadaResponseDTO>> listarPorPaciente(
            @PathVariable Long pacienteId,
            @RequestParam Long odontologoId,
            @RequestParam(defaultValue = "false") boolean esAdmin) {
        log.info("[GET] /api/practicas-realizadas/paciente/{} - Consulta de prácticas por paciente (odontologoId: {}, esAdmin: {})",
                pacienteId, odontologoId, esAdmin);
        return ResponseEntity.ok(turnoPracticaRealizadaService.listarPorPaciente(pacienteId, odontologoId, esAdmin));
    }
}
