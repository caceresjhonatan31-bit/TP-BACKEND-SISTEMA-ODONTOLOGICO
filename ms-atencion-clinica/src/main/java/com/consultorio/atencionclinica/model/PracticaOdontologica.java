package com.consultorio.atencionclinica.model;

import jakarta.persistence.*;

/**
 * Entidad JPA que representa el catálogo (nomenclador) de prácticas odontológicas.
 * Mapeada a la tabla `practicas_odontologicas` en db_atencion_clinica.
 *
 * RN: El código nomenclador es único. La baja es lógica (activo = false)
 *     para inhabilitar la práctica en turnos futuros preservando el historial.
 */
@Entity
@Table(name = "practicas_odontologicas")
public class PracticaOdontologica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código único del nomenclador odontológico (ej: "0101", "0201A").
     * VARCHAR(20) según DER definitivo.
     */
    @Column(name = "codigo_nomenclador", nullable = false, unique = true, length = 20)
    private String codigoNomenclador;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    /**
     * Estado de vigencia del ítem nomenclador.
     * false = inhabilitado (no se puede asignar en nuevos turnos, pero se conserva en historial).
     * RN-10, RN-11: Solo se pueden registrar prácticas con activo = true.
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // ── Constructores ────────────────────────────────────────────────────────

    public PracticaOdontologica() {}

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getCodigoNomenclador() { return codigoNomenclador; }
    public void setCodigoNomenclador(String codigoNomenclador) { this.codigoNomenclador = codigoNomenclador; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
