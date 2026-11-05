package com.consultorio.atencionclinica.service;

import com.consultorio.atencionclinica.dto.request.PacienteRequestDTO;
import com.consultorio.atencionclinica.dto.response.PacienteResponseDTO;
import com.consultorio.atencionclinica.dto.response.ValidacionVigenciaResponseDTO;
import com.consultorio.atencionclinica.model.Paciente;
import com.consultorio.atencionclinica.repository.PacienteRepository;
import com.consultorio.common.exception.ConflictException;
import com.consultorio.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio de negocio para la gestión de pacientes.
 *
 * RN-13: Los pacientes no se eliminan físicamente. Se usa baja lógica (activo = false).
 *         Si un paciente regresa, se reactiva su ficha original en lugar de crear un duplicado.
 */
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
     * Lanza {@link ConflictException} si ya existe un paciente (activo o inactivo) con el mismo DNI.
     * En ese caso se debe usar {@link #reactivarPaciente(Long)} si el paciente estaba de baja.
     */
    public PacienteResponseDTO crearPaciente(PacienteRequestDTO dto) {
        if (pacienteRepository.existsByDni(dto.getDni())) {
            Paciente existente = pacienteRepository.findByDni(dto.getDni()).get();
            String estado = existente.getActivo() ? "activo" : "dado de baja";
            throw new ConflictException(
                    "Ya existe un paciente con el DNI '" + dto.getDni() + "' (" + estado +
                    "). Si está dado de baja, use el endpoint de reactivación.");
        }

        Paciente nuevo = new Paciente();
        nuevo.setDni(dto.getDni());
        nuevo.setNombre(dto.getNombre());
        nuevo.setApellido(dto.getApellido());
        nuevo.setTelefono(dto.getTelefono());
        nuevo.setEmail(dto.getEmail());
        nuevo.setActivo(true);

        return toResponseDTO(pacienteRepository.save(nuevo));
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PacienteResponseDTO obtenerPorId(Long id) {
        return toResponseDTO(buscarPorIdOLanzar(id));
    }

    @Transactional(readOnly = true)
    public PacienteResponseDTO obtenerPorDni(String dni) {
        Paciente paciente = pacienteRepository.findByDni(dni)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ningún paciente con el DNI: " + dni));
        return toResponseDTO(paciente);
    }

    @Transactional(readOnly = true)
    public List<PacienteResponseDTO> listarActivos() {
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
        return pacienteRepository
                .findByApellidoContainingIgnoreCaseOrNombreContainingIgnoreCase(termino, termino)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    // ── Actualización ─────────────────────────────────────────────────────────

    /**
     * Actualiza los datos de contacto de un paciente (teléfono, email).
     * El DNI no se puede cambiar (es la clave de negocio).
     */
    public PacienteResponseDTO actualizarPaciente(Long id, PacienteRequestDTO dto) {
        Paciente paciente = buscarPorIdOLanzar(id);

        // Si el DNI enviado difiere del actual, verificar que no esté en uso por otro
        if (!paciente.getDni().equals(dto.getDni()) && pacienteRepository.existsByDni(dto.getDni())) {
            throw new ConflictException(
                    "El DNI '" + dto.getDni() + "' ya está registrado para otro paciente.");
        }

        paciente.setDni(dto.getDni());
        paciente.setNombre(dto.getNombre());
        paciente.setApellido(dto.getApellido());
        paciente.setTelefono(dto.getTelefono());
        paciente.setEmail(dto.getEmail());

        return toResponseDTO(pacienteRepository.save(paciente));
    }

    // ── Baja lógica (RN-13) ───────────────────────────────────────────────────

    /**
     * Da de baja lógica al paciente (activo = false).
     * No elimina el registro físicamente.
     */
    public void darDeBaja(Long id) {
        Paciente paciente = buscarPorIdOLanzar(id);
        if (!paciente.getActivo()) {
            throw new ConflictException("El paciente con ID " + id + " ya está dado de baja.");
        }
        paciente.setActivo(false);
        pacienteRepository.save(paciente);
    }

    /**
     * Reactiva un paciente previamente dado de baja (RN-13).
     * Recupera su ficha original sin modificar fecha_alta ni historial clínico.
     */
    public PacienteResponseDTO reactivarPaciente(Long id) {
        Paciente paciente = buscarPorIdOLanzar(id);
        if (paciente.getActivo()) {
            throw new ConflictException("El paciente con ID " + id + " ya se encuentra activo.");
        }
        paciente.setActivo(true);
        return toResponseDTO(pacienteRepository.save(paciente));
    }

    // ── Validación inter-servicio ─────────────────────────────────────────────

    /**
     * Verifica si un paciente existe y está activo.
     * Consumido por ms-agenda-usuarios antes de asignar un turno (endpoint GET /api/pacientes/{id}/validar-activo).
     */
    @Transactional(readOnly = true)
    public ValidacionVigenciaResponseDTO validarPacienteActivo(Long id) {
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
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró ningún paciente con ID: " + id));
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
