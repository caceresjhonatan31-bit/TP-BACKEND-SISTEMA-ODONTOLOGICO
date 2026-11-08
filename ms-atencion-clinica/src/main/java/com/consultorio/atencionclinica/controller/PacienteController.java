package com.consultorio.atencionclinica.controller;

import com.consultorio.atencionclinica.dto.request.PacienteRequestDTO;
import com.consultorio.atencionclinica.dto.response.PacienteResponseDTO;
import com.consultorio.atencionclinica.dto.response.ValidacionVigenciaResponseDTO;
import com.consultorio.atencionclinica.service.PacienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de pacientes y su padrón.
 *
 * Mapeo: /api/pacientes
 * RN-13: Baja lógica y reactivación de pacientes sin pérdida de historial.
 */
@Slf4j
@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    @PostMapping
    public ResponseEntity<PacienteResponseDTO> crearPaciente(@Valid @RequestBody PacienteRequestDTO dto) {
        log.info("[POST] /api/pacientes - Solicitud de creación de paciente");
        PacienteResponseDTO nuevo = pacienteService.crearPaciente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @GetMapping
    public ResponseEntity<List<PacienteResponseDTO>> listarPacientes(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String dni) {

        if (dni != null && !dni.isBlank()) {
            log.info("[GET] /api/pacientes - Consulta de paciente filtrado por DNI");
            return ResponseEntity.ok(List.of(pacienteService.obtenerPorDni(dni.trim())));
        }

        if (busqueda != null && !busqueda.isBlank()) {
            log.info("[GET] /api/pacientes - Consulta de pacientes por criterio de búsqueda");
            return ResponseEntity.ok(pacienteService.buscarPorNombreOApellido(busqueda.trim()));
        }

        log.info("[GET] /api/pacientes - Listado de pacientes activos");
        return ResponseEntity.ok(pacienteService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponseDTO> obtenerPorId(@PathVariable Long id) {
        log.info("[GET] /api/pacientes/{} - Consulta de paciente por ID", id);
        return ResponseEntity.ok(pacienteService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PacienteResponseDTO> actualizarPaciente(
            @PathVariable Long id,
            @Valid @RequestBody PacienteRequestDTO dto) {
        log.info("[PUT] /api/pacientes/{} - Solicitud de actualización de paciente", id);
        return ResponseEntity.ok(pacienteService.actualizarPaciente(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        log.info("[DELETE] /api/pacientes/{} - Solicitud de baja lógica de paciente", id);
        pacienteService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<PacienteResponseDTO> reactivarPaciente(@PathVariable Long id) {
        log.info("[PATCH] /api/pacientes/{}/reactivar - Solicitud de reactivación de paciente", id);
        return ResponseEntity.ok(pacienteService.reactivarPaciente(id));
    }

    @GetMapping("/{id}/validar-activo")
    public ResponseEntity<ValidacionVigenciaResponseDTO> validarPacienteActivo(@PathVariable Long id) {
        log.info("[GET] /api/pacientes/{}/validar-activo - Validación inter-servicio de paciente", id);
        return ResponseEntity.ok(pacienteService.validarPacienteActivo(id));
    }
}
