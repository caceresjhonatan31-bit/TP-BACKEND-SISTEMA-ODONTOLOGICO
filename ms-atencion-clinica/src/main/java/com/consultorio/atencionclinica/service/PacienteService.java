package com.consultorio.atencionclinica.service;

import com.consultorio.atencionclinica.dto.request.PacienteRequestDTO;
import com.consultorio.atencionclinica.dto.response.PacienteResponseDTO;
import com.consultorio.atencionclinica.dto.response.ValidacionVigenciaResponseDTO;
import com.consultorio.atencionclinica.model.Paciente;
import com.consultorio.atencionclinica.repository.PacienteRepository;
import com.consultorio.common.exception.ConflictException;
import com.consultorio.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para la gestión de pacientes.
 *
 * RN-13: Los pacientes no se eliminan físicamente. Se usa baja lógica (activo = false).
 *         Si un paciente regresa, se reactiva su ficha original en lugar de crear un duplicado.
 */
@Slf4j
@Service
@Transactional
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    // ── Creación ─────────────────────────────────────────────────────────────

    /**
     * Registra un nuevo paciente en el sistema.
     * Lanza {@link ConflictException} si ya existe un paciente con el mismo DNI.
     */
    public PacienteResponseDTO crearPaciente(PacienteRequestDTO dto) {
        log.debug("Iniciando validaciones para registro de nuevo paciente");

        if (pacienteRepository.existsByDni(dto.getDni())) {
            Paciente existente = pacienteRepository.findByDni(dto.getDni()).get();
            String estado = existente.getActivo() ? "activo" : "dado de baja";
            log.warn("Rechazo de creación: DNI duplicado para paciente existente (estado: {})", estado);
            throw new ConflictException(
                    "Ya existe un paciente registrado con el DNI indicado (" + estado +
                    "). Si está dado de baja, use el endpoint de reactivación.");
        }

        Paciente nuevo = new Paciente();
        nuevo.setDni(dto.getDni());
        nuevo.setNombre(dto.getNombre());
        nuevo.setApellido(dto.getApellido());
        nuevo.setTelefono(dto.getTelefono());
        nuevo.setEmail(dto.getEmail());
        nuevo.setActivo(true);

        Paciente guardado = pacienteRepository.save(nuevo);
        log.info("Paciente creado exitosamente con ID: {}", guardado.getId());
        return toResponseDTO(guardado);
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PacienteResponseDTO obtenerPorId(Long id) {
        log.debug("Consultando paciente por ID: {}", id);
        return toResponseDTO(buscarPorIdOLanzar(id));
    }

    @Transactional(readOnly = true)
    public PacienteResponseDTO obtenerPorDni(String dni) {
        log.debug("Consultando paciente por DNI");
        Paciente paciente = pacienteRepository.findByDni(dni)
                .orElseThrow(() -> {
                    log.warn("Paciente no encontrado para el DNI solicitado");
                    return new ResourceNotFoundException("No se encontró ningún paciente con el DNI indicado");
                });
        return toResponseDTO(paciente);
    }

    @Transactional(readOnly = true)
    public List<PacienteResponseDTO> listarActivos() {
        log.debug("Consultando listado de pacientes activos");
        return pacienteRepository.findByActivoTrue()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Búsqueda flexible por apellido o nombre (case-insensitive, coincidencia parcial).
     * Requerida por F-01: "Busca ficha por nombre/apellido" (RN-13).
     */
    @Transactional(readOnly = true)
    public List<PacienteResponseDTO> buscarPorNombreOApellido(String termino) {
        log.debug("Buscando pacientes por criterio de coincidencia de nombre/apellido");
        return pacienteRepository
                .findByApellidoContainingIgnoreCaseOrNombreContainingIgnoreCase(termino, termino)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ── Actualización ─────────────────────────────────────────────────────────

    /**
     * Actualiza los datos de contacto de un paciente (teléfono, email).
     */
    public PacienteResponseDTO actualizarPaciente(Long id, PacienteRequestDTO dto) {
        log.debug("Iniciando validación de datos para actualizar paciente ID: {}", id);
        Paciente paciente = buscarPorIdOLanzar(id);

        if (!paciente.getDni().equals(dto.getDni()) && pacienteRepository.existsByDni(dto.getDni())) {
            log.warn("Rechazo de actualización en paciente ID {}: DNI ya registrado para otro paciente", id);
            throw new ConflictException("El DNI indicado ya está registrado para otro paciente.");
        }

        paciente.setDni(dto.getDni());
        paciente.setNombre(dto.getNombre());
        paciente.setApellido(dto.getApellido());
        paciente.setTelefono(dto.getTelefono());
        paciente.setEmail(dto.getEmail());

        Paciente actualizado = pacienteRepository.save(paciente);
        log.info("Paciente ID {} actualizado exitosamente", actualizado.getId());
        return toResponseDTO(actualizado);
    }

    // ── Baja lógica (RN-13) ───────────────────────────────────────────────────

    /**
     * Da de baja lógica al paciente (activo = false).
     */
    public void darDeBaja(Long id) {
        log.debug("Iniciando baja lógica para paciente ID: {}", id);
        Paciente paciente = buscarPorIdOLanzar(id);
        if (!paciente.getActivo()) {
            log.warn("Rechazo de baja lógica: Paciente ID {} ya se encuentra dado de baja (RN-13)", id);
            throw new ConflictException("El paciente con ID " + id + " ya está dado de baja.");
        }
        paciente.setActivo(false);
        pacienteRepository.save(paciente);
        log.info("Paciente ID {} dado de baja lógica exitosamente (RN-13)", id);
    }

    /**
     * Reactiva un paciente previamente dado de baja (RN-13).
     */
    public PacienteResponseDTO reactivarPaciente(Long id) {
        log.debug("Iniciando reactivación para paciente ID: {}", id);
        Paciente paciente = buscarPorIdOLanzar(id);
        if (paciente.getActivo()) {
            log.warn("Rechazo de reactivación: Paciente ID {} ya se encuentra activo (RN-13)", id);
            throw new ConflictException("El paciente con ID " + id + " ya se encuentra activo.");
        }
        paciente.setActivo(true);
        Paciente reactivado = pacienteRepository.save(paciente);
        log.info("Paciente ID {} reactivado exitosamente (RN-13)", reactivado.getId());
        return toResponseDTO(reactivado);
    }

    // ── Validación inter-servicio ─────────────────────────────────────────────

    /**
     * Verifica si un paciente existe y está activo para ms-agenda-usuarios.
     */
    @Transactional(readOnly = true)
    public ValidacionVigenciaResponseDTO validarPacienteActivo(Long id) {
        log.debug("Validando estado de vigencia para paciente ID: {}", id);
        return pacienteRepository.findById(id)
                .map(p -> ValidacionVigenciaResponseDTO.builder()
                        .id(p.getId())
                        .vigente(p.getActivo())
                        .detalle(p.getActivo()
                                ? "Paciente activo: " + p.getApellido() + ", " + p.getNombre()
                                : "Paciente dado de baja. Debe ser reactivado antes de asignar un turno.")
                        .build())
                .orElse(ValidacionVigenciaResponseDTO.builder()
                        .id(id)
                        .vigente(false)
                        .detalle("No existe ningún paciente registrado con ID: " + id)
                        .build());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Paciente buscarPorIdOLanzar(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Paciente ID {} no encontrado en base de datos", id);
                    return new ResourceNotFoundException("No se encontró ningún paciente con ID: " + id);
                });
    }

    private PacienteResponseDTO toResponseDTO(Paciente p) {
        return PacienteResponseDTO.builder()
                .id(p.getId())
                .dni(p.getDni())
                .nombre(p.getNombre())
                .apellido(p.getApellido())
                .telefono(p.getTelefono())
                .email(p.getEmail())
                .activo(p.getActivo())
                .fechaAlta(p.getFechaAlta())
                .build();
    }
}
