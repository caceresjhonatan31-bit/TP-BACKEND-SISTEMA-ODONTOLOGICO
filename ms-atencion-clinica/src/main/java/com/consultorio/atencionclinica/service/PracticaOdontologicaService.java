package com.consultorio.atencionclinica.service;

import com.consultorio.atencionclinica.dto.request.PracticaOdontologicaRequestDTO;
import com.consultorio.atencionclinica.dto.response.PracticaOdontologicaResponseDTO;
import com.consultorio.atencionclinica.dto.response.ValidacionVigenciaResponseDTO;
import com.consultorio.atencionclinica.model.PracticaOdontologica;
import com.consultorio.atencionclinica.repository.PracticaOdontologicaRepository;
import com.consultorio.common.exception.ConflictException;
import com.consultorio.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para el catálogo nomenclado de prácticas odontológicas.
 *
 * RN-10 / RN-11: Solo las prácticas con activo = true pueden usarse en actos médicos.
 * La baja es lógica: inhabilita la práctica para turnos futuros conservando el historial.
 */
@Service
@Transactional
public class PracticaOdontologicaService {

    private final PracticaOdontologicaRepository practicaRepository;

    public PracticaOdontologicaService(PracticaOdontologicaRepository practicaRepository) {
        this.practicaRepository = practicaRepository;
    }

    // ── Creación ─────────────────────────────────────────────────────────────

    /**
     * Agrega una nueva práctica al nomenclador.
     * Lanza {@link ConflictException} si el código nomenclador ya existe.
     */
    public PracticaOdontologicaResponseDTO crearPractica(PracticaOdontologicaRequestDTO dto) {
        if (practicaRepository.existsByCodigoNomenclador(dto.getCodigoNomenclador())) {
            throw new ConflictException(
                    "Ya existe una práctica con el código nomenclador: " + dto.getCodigoNomenclador());
        }

        PracticaOdontologica nueva = new PracticaOdontologica();
        nueva.setCodigoNomenclador(dto.getCodigoNomenclador());
        nueva.setNombre(dto.getNombre());
        nueva.setDescripcion(dto.getDescripcion());
        nueva.setActivo(true);

        return toResponseDTO(practicaRepository.save(nueva));
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PracticaOdontologicaResponseDTO obtenerPorId(Long id) {
        return toResponseDTO(buscarPorIdOLanzar(id));
    }

    @Transactional(readOnly = true)
    public List<PracticaOdontologicaResponseDTO> listarTodas() {
        return practicaRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PracticaOdontologicaResponseDTO> listarActivas() {
        return practicaRepository.findByActivoTrue()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ── Actualización ─────────────────────────────────────────────────────────

    /**
     * Actualiza los datos descriptivos de una práctica del nomenclador.
     * Si se cambia el código, verifica que el nuevo no esté en uso.
     */
    public PracticaOdontologicaResponseDTO actualizarPractica(Long id, PracticaOdontologicaRequestDTO dto) {
        PracticaOdontologica practica = buscarPorIdOLanzar(id);

        if (!practica.getCodigoNomenclador().equals(dto.getCodigoNomenclador())
                && practicaRepository.existsByCodigoNomenclador(dto.getCodigoNomenclador())) {
            throw new ConflictException(
                    "El código nomenclador '" + dto.getCodigoNomenclador() + "' ya está en uso.");
        }

        practica.setCodigoNomenclador(dto.getCodigoNomenclador());
        practica.setNombre(dto.getNombre());
        practica.setDescripcion(dto.getDescripcion());

        return toResponseDTO(practicaRepository.save(practica));
    }

    // ── Baja / Reactivación lógica ───────────────────────────────────────────

    /** Inhabilita una práctica para nuevos turnos. No elimina el historial. */
    public void darDeBaja(Long id) {
        PracticaOdontologica practica = buscarPorIdOLanzar(id);
        if (!practica.getActivo()) {
            throw new ConflictException("La práctica con ID " + id + " ya está inhabilitada.");
        }
        practica.setActivo(false);
        practicaRepository.save(practica);
    }

    /** Reactiva una práctica inhabilitada para que pueda volver a asignarse. */
    public PracticaOdontologicaResponseDTO reactivarPractica(Long id) {
        PracticaOdontologica practica = buscarPorIdOLanzar(id);
        if (practica.getActivo()) {
            throw new ConflictException("La práctica con ID " + id + " ya está activa.");
        }
        practica.setActivo(true);
        return toResponseDTO(practicaRepository.save(practica));
    }

    // ── Validación inter-servicio ─────────────────────────────────────────────

    /**
     * Verifica si una práctica existe y está vigente (activo = true).
     * Consumido por ms-agenda-usuarios: GET /api/practicas/{id}/validar-activa.
     */
    @Transactional(readOnly = true)
    public ValidacionVigenciaResponseDTO validarPracticaActiva(Long id) {
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ninguna práctica con ID: " + id));
    }

    public PracticaOdontologica buscarEntidadPorIdActivaOLanzar(Long id) {
        return practicaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ConflictException(
                        "La práctica con ID " + id + " no existe o está inhabilitada (RN-10)."));
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
