package com.consultorio.atencionclinica.controller;

import com.consultorio.atencionclinica.dto.request.EvolucionClinicaRequestDTO;
import com.consultorio.atencionclinica.dto.response.EvolucionClinicaResponseDTO;
import com.consultorio.atencionclinica.service.EvolucionClinicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para el registro y consulta de notas y evoluciones clínicas.
 *
 * Mapeo: /api/evoluciones
 * RN-15: Continuidad de sesiones clínicas.
 * RN-16: Confidencialidad de la historia clínica.
 */
@RestController
@RequestMapping("/api/evoluciones")
@RequiredArgsConstructor
public class EvolucionClinicaController {

    private final EvolucionClinicaService evolucionClinicaService;

    @PostMapping
    public ResponseEntity<EvolucionClinicaResponseDTO> registrarEvolucion(
            @Valid @RequestBody EvolucionClinicaRequestDTO dto) {
        EvolucionClinicaResponseDTO nueva = evolucionClinicaService.registrarEvolucion(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvolucionClinicaResponseDTO> actualizarEvolucion(
            @PathVariable Long id,
            @Valid @RequestBody EvolucionClinicaRequestDTO dto) {
        return ResponseEntity.ok(evolucionClinicaService.actualizarEvolucion(id, dto));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<EvolucionClinicaResponseDTO>> listarPorPaciente(
            @PathVariable Long pacienteId,
            @RequestParam Long odontologoId,
            @RequestParam(defaultValue = "false") boolean esAdmin) {
        return ResponseEntity.ok(evolucionClinicaService.listarPorPaciente(pacienteId, odontologoId, esAdmin));
    }
}
