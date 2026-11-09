package com.consultorio.atencionclinica.controller;

import com.consultorio.atencionclinica.dto.request.EvolucionClinicaRequestDTO;
import com.consultorio.atencionclinica.dto.response.EvolucionClinicaResponseDTO;
import com.consultorio.atencionclinica.service.EvolucionClinicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/evoluciones")
@RequiredArgsConstructor
public class EvolucionClinicaController {

    private final EvolucionClinicaService evolucionClinicaService;

    @PostMapping
    public ResponseEntity<EvolucionClinicaResponseDTO> registrarEvolucion(
            @Valid @RequestBody EvolucionClinicaRequestDTO dto) {
        log.info("[POST] /api/evoluciones - Registro de evolución clínica (pacienteId: {}, turnoDesdeId: {})",
                dto.getPacienteId(), dto.getTurnoDesdeId());
        EvolucionClinicaResponseDTO nueva = evolucionClinicaService.registrarEvolucion(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EvolucionClinicaResponseDTO> actualizarEvolucion(
            @PathVariable Long id,
            @Valid @RequestBody EvolucionClinicaRequestDTO dto) {
        log.info("[PUT] /api/evoluciones/{} - Actualización de evolución clínica", id);
        return ResponseEntity.ok(evolucionClinicaService.actualizarEvolucion(id, dto));
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<List<EvolucionClinicaResponseDTO>> listarPorPaciente(
            @PathVariable Long pacienteId,
            @RequestParam Long odontologoId,
            @RequestParam(defaultValue = "false") boolean esAdmin) {
        log.info("[GET] /api/evoluciones/paciente/{} - Consulta de historia clínica (pacienteId: {}, odontologoId: {}, esAdmin: {})",
                pacienteId, odontologoId, esAdmin);
        return ResponseEntity.ok(evolucionClinicaService.listarPorPaciente(pacienteId, odontologoId, esAdmin));
    }
}
