package com.consultorio.atencionclinica.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "evoluciones_clinicas")
public class EvolucionClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
     //FK al paciente dentro de este mismo microservicio
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_evolucion_paciente"))
    private Paciente paciente;

    @Column(name = "turno_desde_id", nullable = false)
    private Long turnoDesdeId;

    @Column(name = "turno_hasta_id")
    private Long turnoHastaId;

    @Column(name = "actualizado_por_odontologo_id", nullable = false)
    private Long actualizadoPorOdontologoId;

    @Column(name = "diagnostico", nullable = false, length = 255)
    private String diagnostico;

    @Column(name = "evolucion_texto", nullable = false, columnDefinition = "TEXT")
    private String evolucionTexto;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    @PreUpdate
    protected void onPersistOrUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    // ── Constructores ────────────────────────────────────────────────────────

    public EvolucionClinica() {}

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }

    public Long getTurnoDesdeId() { return turnoDesdeId; }
    public void setTurnoDesdeId(Long turnoDesdeId) { this.turnoDesdeId = turnoDesdeId; }

    public Long getTurnoHastaId() { return turnoHastaId; }
    public void setTurnoHastaId(Long turnoHastaId) { this.turnoHastaId = turnoHastaId; }

    public Long getActualizadoPorOdontologoId() { return actualizadoPorOdontologoId; }
    public void setActualizadoPorOdontologoId(Long actualizadoPorOdontologoId) {
        this.actualizadoPorOdontologoId = actualizadoPorOdontologoId;
    }

    public String getDiagnostico() { return diagnostico; }
    public void setDiagnostico(String diagnostico) { this.diagnostico = diagnostico; }

    public String getEvolucionTexto() { return evolucionTexto; }
    public void setEvolucionTexto(String evolucionTexto) { this.evolucionTexto = evolucionTexto; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
