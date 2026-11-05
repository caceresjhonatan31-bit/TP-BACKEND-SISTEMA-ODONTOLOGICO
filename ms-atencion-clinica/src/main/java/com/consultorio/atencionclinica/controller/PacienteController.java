package com.consultorio.atencionclinica.controller;

import com.consultorio.atencionclinica.dto.request.PacienteRequestDTO;
import com.consultorio.atencionclinica.dto.response.PacienteResponseDTO;
import com.consultorio.atencionclinica.dto.response.ValidacionVigenciaResponseDTO;
import com.consultorio.atencionclinica.service.PacienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    @PostMapping
    public ResponseEntity<PacienteResponseDTO> crearPaciente(@Valid @RequestBody PacienteRequestDTO dto) {
        PacienteResponseDTO nuevo = pacienteService.crearPaciente(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @GetMapping
    public ResponseEntity<List<PacienteResponseDTO>> listarPacientes(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String dni) {

        if (dni != null && !dni.isBlank()) {
            return ResponseEntity.ok(List.of(pacienteService.obtenerPorDni(dni.trim())));
        }

        if (busqueda != null && !busqueda.isBlank()) {
            return ResponseEntity.ok(pacienteService.buscarPorNombreOApellido(busqueda.trim()));
        }

        return ResponseEntity.ok(pacienteService.listarActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PacienteResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PacienteResponseDTO> actualizarPaciente(
            @PathVariable Long id,
            @Valid @RequestBody PacienteRequestDTO dto) {
        return ResponseEntity.ok(pacienteService.actualizarPaciente(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        pacienteService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivar")
    public ResponseEntity<PacienteResponseDTO> reactivarPaciente(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.reactivarPaciente(id));
    }

    @GetMapping("/{id}/validar-activo")
    public ResponseEntity<ValidacionVigenciaResponseDTO> validarPacienteActivo(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.validarPacienteActivo(id));
    }
}
