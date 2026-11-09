package com.consultorio.atencionclinica.controller;

import com.consultorio.atencionclinica.dto.request.PracticaOdontologicaRequestDTO;
import com.consultorio.atencionclinica.dto.response.PracticaOdontologicaResponseDTO;
import com.consultorio.atencionclinica.dto.response.ValidacionVigenciaResponseDTO;
import com.consultorio.atencionclinica.service.PracticaOdontologicaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/practicas")
@RequiredArgsConstructor
public class PracticaOdontologicaController {

    private final PracticaOdontologicaService practicaService;

    @PostMapping
    public ResponseEntity<PracticaOdontologicaResponseDTO> crearPractica(
            @Valid @RequestBody PracticaOdontologicaRequestDTO dto) {
        log.info("[POST] /api/practicas - Solicitud de creación de práctica nomenclada: {}", dto.getCodigoNomenclador());
        PracticaOdontologicaResponseDTO nueva = practicaService.crearPractica(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @GetMapping
    public ResponseEntity<List<PracticaOdontologicaResponseDTO>> listarPracticas(
            @RequestParam(defaultValue = "true") boolean soloActivas) {
        log.info("[GET] /api/practicas - Listado de prácticas (soloActivas: {})", soloActivas);
        List<PracticaOdontologicaResponseDTO> practicas = soloActivas
                ? practicaService.listarActivas()
                : practicaService.listarTodas();
        return ResponseEntity.ok(practicas);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PracticaOdontologicaResponseDTO> obtenerPorId(@PathVariable Long id) {
        log.info("[GET] /api/practicas/{} - Consulta de práctica por ID", id);
        return ResponseEntity.ok(practicaService.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PracticaOdontologicaResponseDTO> actualizarPractica(
            @PathVariable Long id,
            @Valid @RequestBody PracticaOdontologicaRequestDTO dto) {
        log.info("[PUT] /api/practicas/{} - Solicitud de actualización de práctica", id);
        return ResponseEntity.ok(practicaService.actualizarPractica(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> inhabilitarPractica(@PathVariable Long id) {
        log.info("[DELETE] /api/practicas/{} - Solicitud de inhabilitación de práctica", id);
        practicaService.darDeBaja(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/habilitar")
    public ResponseEntity<PracticaOdontologicaResponseDTO> reactivarPractica(@PathVariable Long id) {
        log.info("[PATCH] /api/practicas/{}/habilitar - Solicitud de habilitación de práctica", id);
        return ResponseEntity.ok(practicaService.reactivarPractica(id));
    }

    @GetMapping("/{id}/validar-activa")
    public ResponseEntity<ValidacionVigenciaResponseDTO> validarPracticaActiva(@PathVariable Long id) {
        log.info("[GET] /api/practicas/{}/validar-activa - Validación inter-servicio de práctica", id);
        return ResponseEntity.ok(practicaService.validarPracticaActiva(id));
    }
}
