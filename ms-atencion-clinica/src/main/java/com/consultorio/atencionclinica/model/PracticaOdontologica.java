package com.consultorio.atencionclinica.model;

import jakarta.persistence.*;


@Entity
@Table(name = "practicas_odontologicas")
public class PracticaOdontologica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_nomenclador", nullable = false, unique = true, length = 20)
    private String codigoNomenclador;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;


    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // ── Constructores ────────────────────────────────────────────────────────

    public PracticaOdontologica() {}

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoNomenclador() { return codigoNomenclador; }
    public void setCodigoNomenclador(String codigoNomenclador) { this.codigoNomenclador = codigoNomenclador; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
