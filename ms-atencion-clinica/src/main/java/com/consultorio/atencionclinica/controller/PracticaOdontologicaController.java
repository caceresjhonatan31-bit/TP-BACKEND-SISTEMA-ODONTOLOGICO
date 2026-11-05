package com.consultorio.atencionclinica.controller;

import com.consultorio.atencionclinica.dto.request.PracticaOdontologicaRequestDTO;
import com.consultorio.atencionclinica.dto.response.PracticaOdontologicaResponseDTO;
import com.consultorio.atencionclinica.dto.response.ValidacionVigenciaResponseDTO;
import com.consultorio.atencionclinica.service.PracticaOdontologicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para el catálogo oficial de prácticas odontológicas (nomenclador).
 *
 * Mapeo: /api/practicas
 * RN-10, RN-11: Catálogo nomenclado y vigencia de prestaciones.
 */
@RestController
@RequestMapping("/api/practicas")
@RequiredArgsConstructor
public class PracticaOdontologicaController {

    private final PracticaOdontologicaService practicaService;

    @PostMapping
    public ResponseEntity<PracticaOdontologicaResponseDTO> crearPractica(
            @Valid @RequestBody PracticaOdontologicaRequestDTO dto) {
        PracticaOdontologicaResponseDTO nueva = practicaService.crearPractica(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @GetMapping
    public ResponseEntity<List<PracticaOdontologicaResponseDTO>> listarPracticas(
            @RequestParam(defaultValue = "true") boolean soloActivas) {
        List<PracticaOdontologicaResponseDTO> practicas = soloActivas
                ? practicaService.listarActivas()
                : practicaService.listarTodas();
        return ResponseEntity.ok(practicas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PracticaOdontologicaResponseDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(practicaService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PracticaOdontologicaResponseDTO> actualizarPractica(
            @PathVariable Long id,
            @Valid @RequestBody PracticaOdontologicaRequestDTO dto) {
        return ResponseEntity.ok(practicaService.actualizarPractica(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inhabilitarPractica(@PathVariable Long id) {
        practicaService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/habilitar")
    public ResponseEntity<PracticaOdontologicaResponseDTO> reactivarPractica(@PathVariable Long id) {
        return ResponseEntity.ok(practicaService.reactivarPractica(id));
    }

    @GetMapping("/{id}/validar-activa")
    public ResponseEntity<ValidacionVigenciaResponseDTO> validarPracticaActiva(@PathVariable Long id) {
        return ResponseEntity.ok(practicaService.validarPracticaActiva(id));
    }
}
