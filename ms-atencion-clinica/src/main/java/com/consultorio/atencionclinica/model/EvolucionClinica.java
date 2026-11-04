package com.consultorio.atencionclinica.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa la nota evolutiva clínica de un paciente.
 * Mapeada a la tabla `evoluciones_clinicas` en db_atencion_clinica.
 *
 * RN-15: turno_hasta_id es opcional (NULL = sesión única). Si se envía,
 *         debe cumplir turno_hasta_id >= turno_desde_id (enforced por CHECK en DB).
 * RN-16: La consulta de evoluciones es confidencial: solo ADMIN o el ODONTÓLOGO
 *         que atendió al paciente al menos una vez puede acceder.
 *
 * Relaciones inter-servicio (Logical FK — sin FK física cruzada):
 *   - turno_desde_id  → turnos.id  en db_agenda_usuarios
 *   - turno_hasta_id  → turnos.id  en db_agenda_usuarios
 *   - actualizado_por_odontologo_id → usuarios.id en db_agenda_usuarios
 */
@Entity
@Table(name = "evoluciones_clinicas")
public class EvolucionClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FK física al paciente dentro de este mismo microservicio (db_atencion_clinica).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_evolucion_paciente"))
    private Paciente paciente;

    /**
     * Referencia lógica al turno inicial del tratamiento en ms-agenda-usuarios.
     * No existe FK física cruzada (patrón Database-per-Service).
     */
    @Column(name = "turno_desde_id", nullable = false)
    private Long turnoDesdeId;

    /**
     * Referencia lógica al turno de cierre del tratamiento (nullable).
     * NULL indica sesión única o tratamiento aún en curso (RN-15).
     */
    @Column(name = "turno_hasta_id")
    private Long turnoHastaId;

    /**
     * Referencia lógica al odontólogo que registró/actualizó esta nota (en db_agenda_usuarios).
     */
    @Column(name = "actualizado_por_odontologo_id", nullable = false)
    private Long actualizadoPorOdontologoId;

    @Column(name = "diagnostico", nullable = false, length = 255)
    private String diagnostico;

    @Column(name = "evolucion_texto", nullable = false, columnDefinition = "TEXT")
    private String evolucionTexto;

    /**
     * Timestamp de la última actualización de esta nota evolutiva.
     * Columna: fecha_actualizacion (DER definitivo).
     */
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
