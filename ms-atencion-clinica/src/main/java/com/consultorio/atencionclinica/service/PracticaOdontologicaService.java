package com.consultorio.atencionclinica.service;

import com.consultorio.atencionclinica.dto.request.PracticaOdontologicaRequestDTO;
import com.consultorio.atencionclinica.dto.response.PracticaOdontologicaResponseDTO;
import com.consultorio.atencionclinica.dto.response.ValidacionVigenciaResponseDTO;
import com.consultorio.atencionclinica.model.PracticaOdontologica;
import com.consultorio.atencionclinica.repository.PracticaOdontologicaRepository;
import com.consultorio.common.exception.ConflictException;
import com.consultorio.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class PracticaOdontologicaService {

    private final PracticaOdontologicaRepository practicaRepository;

    public PracticaOdontologicaService(PracticaOdontologicaRepository practicaRepository) {
        this.practicaRepository = practicaRepository;
    }

    // ── Creación ─────────────────────────────────────────────────────────────


    public PracticaOdontologicaResponseDTO crearPractica(PracticaOdontologicaRequestDTO dto) {
        log.debug("Iniciando validación para crear práctica con código: {}", dto.getCodigoNomenclador());

        if (practicaRepository.existsByCodigoNomenclador(dto.getCodigoNomenclador())) {
            log.warn("Rechazo de creación: Código nomenclador '{}' duplicado", dto.getCodigoNomenclador());
            throw new ConflictException(
                    "Ya existe una práctica con el código nomenclador: " + dto.getCodigoNomenclador());
        }

        PracticaOdontologica nueva = new PracticaOdontologica();
        nueva.setCodigoNomenclador(dto.getCodigoNomenclador());
        nueva.setNombre(dto.getNombre());
        nueva.setDescripcion(dto.getDescripcion());
        nueva.setActivo(true);

        PracticaOdontologica guardada = practicaRepository.save(nueva);
        log.info("Práctica creada exitosamente con ID: {}, código: {}", guardada.getId(), guardada.getCodigoNomenclador());
        return toResponseDTO(guardada);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PracticaOdontologicaResponseDTO obtenerPorId(Long id) {
        log.debug("Consultando práctica por ID: {}", id);
        return toResponseDTO(buscarPorIdOLanzar(id));
    }

    @Transactional(readOnly = true)
    public List<PracticaOdontologicaResponseDTO> listarTodas() {
        log.debug("Consultando catálogo completo de prácticas");
        return practicaRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PracticaOdontologicaResponseDTO> listarActivas() {
        log.debug("Consultando catálogo de prácticas activas");
        return practicaRepository.findByActivoTrue()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ── Actualización ─────────────────────────────────────────────────────────

    public PracticaOdontologicaResponseDTO actualizarPractica(Long id, PracticaOdontologicaRequestDTO dto) {
        log.debug("Iniciando validación para actualizar práctica ID: {}", id);
        PracticaOdontologica practica = buscarPorIdOLanzar(id);

        if (!practica.getCodigoNomenclador().equals(dto.getCodigoNomenclador())
                && practicaRepository.existsByCodigoNomenclador(dto.getCodigoNomenclador())) {
            log.warn("Rechazo de actualización en práctica ID {}: Código nomenclador '{}' ya en uso",
                    id, dto.getCodigoNomenclador());
            throw new ConflictException(
                    "El código nomenclador '" + dto.getCodigoNomenclador() + "' ya está en uso.");
        }

        practica.setCodigoNomenclador(dto.getCodigoNomenclador());
        practica.setNombre(dto.getNombre());
        practica.setDescripcion(dto.getDescripcion());

        PracticaOdontologica actualizada = practicaRepository.save(practica);
        log.info("Práctica ID {} actualizada exitosamente", actualizada.getId());
        return toResponseDTO(actualizada);
    }

    // ── Baja lógica ───────────────────────────────────────────

    /** Inhabilita una práctica para nuevos turnos. */
    public void darDeBaja(Long id) {
        log.debug("Iniciando inhabilitación de práctica ID: {}", id);
        PracticaOdontologica practica = buscarPorIdOLanzar(id);
        if (!practica.getActivo()) {
            log.warn("Rechazo de inhabilitación: Práctica ID {} ya está inhabilitada", id);
            throw new ConflictException("La práctica con ID " + id + " ya está inhabilitada.");
        }
        practica.setActivo(false);
        practicaRepository.save(practica);
        log.info("Práctica ID {} inhabilitada exitosamente (baja lógica)", id);
    }

    /** Reactiva una práctica inhabilitada para que pueda volver a asignarse. */
    public PracticaOdontologicaResponseDTO reactivarPractica(Long id) {
        log.debug("Iniciando reactivación de práctica ID: {}", id);
        PracticaOdontologica practica = buscarPorIdOLanzar(id);
        if (practica.getActivo()) {
            log.warn("Rechazo de habilitación: Práctica ID {} ya está activa", id);
            throw new ConflictException("La práctica con ID " + id + " ya está activa.");
        }
        practica.setActivo(true);
        PracticaOdontologica reactivada = practicaRepository.save(practica);
        log.info("Práctica ID {} habilitada exitosamente", reactivada.getId());
        return toResponseDTO(reactivada);
    }

    // ── Validación inter-servicio ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ValidacionVigenciaResponseDTO validarPracticaActiva(Long id) {
        log.debug("Validando estado de vigencia para práctica ID: {}", id);
        return practicaRepository.findById(id)
                .map(p -> ValidacionVigenciaResponseDTO.builder()
                        .id(p.getId())
                        .vigente(p.getActivo())
                        .detalle(p.getActivo()
                                ? "Práctica vigente: [" + p.getCodigoNomenclador() + "] " + p.getNombre()
                                : "Práctica inhabilitada. No puede usarse en nuevos turnos.")
                        .build())
                .orElse(ValidacionVigenciaResponseDTO.builder()
                        .id(id)
                        .vigente(false)
                        .detalle("No existe ninguna práctica registrada con ID: " + id)
                        .build());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private PracticaOdontologica buscarPorIdOLanzar(Long id) {
        return practicaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Práctica ID {} no encontrada en base de datos", id);
                    return new ResourceNotFoundException("No se encontró ninguna práctica con ID: " + id);
                });
    }

    public PracticaOdontologica buscarEntidadPorIdActivaOLanzar(Long id) {
        return practicaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> {
                    log.warn("Práctica ID {} no disponible o inhabilitada para acto médico (RN-10)", id);
                    return new ConflictException("La práctica con ID " + id + " no existe o está inhabilitada (RN-10).");
                });
    }

    private PracticaOdontologicaResponseDTO toResponseDTO(PracticaOdontologica p) {
        return PracticaOdontologicaResponseDTO.builder()
                .id(p.getId())
                .codigoNomenclador(p.getCodigoNomenclador())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .activo(p.getActivo())
                .build();
    }
}
