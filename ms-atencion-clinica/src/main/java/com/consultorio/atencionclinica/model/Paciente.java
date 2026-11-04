package com.consultorio.atencionclinica.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa la ficha de un paciente del consultorio.
 * Mapeada a la tabla `pacientes` en db_atencion_clinica.
 *
 * RN-13: Los pacientes no se eliminan físicamente. Se usa baja lógica (activo = false).
 *         Un paciente dado de baja puede ser reactivado recuperando su ficha original.
 */
@Entity
@Table(name = "pacientes")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * DNI único del paciente. Usado para búsqueda y como clave de negocio.
     * Permite reactivar paciente existente si regresa (RN-13).
     */
    @Column(name = "dni", nullable = false, unique = true, length = 20)
    private String dni;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "apellido", nullable = false, length = 100)
    private String apellido;

    @Column(name = "telefono", nullable = false, length = 30)
    private String telefono;

    @Column(name = "email", length = 150)
    private String email;

    /**
     * Estado lógico del paciente. false = dado de baja.
     * El DELETE físico no está permitido (RN-13).
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /**
     * Timestamp de alta inicial del paciente en el sistema.
     * Se setea una sola vez al crearlo; no se modifica en reactivaciones.
     */
    @Column(name = "fecha_alta", nullable = false, updatable = false)
    private LocalDateTime fechaAlta;

    @PrePersist
    protected void onCreate() {
        if (this.fechaAlta == null) {
            this.fechaAlta = LocalDateTime.now();
        }
        if (this.activo == null) {
            this.activo = true;
        }
    }

    // ── Constructores ────────────────────────────────────────────────────────

    public Paciente() {}

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }
}
