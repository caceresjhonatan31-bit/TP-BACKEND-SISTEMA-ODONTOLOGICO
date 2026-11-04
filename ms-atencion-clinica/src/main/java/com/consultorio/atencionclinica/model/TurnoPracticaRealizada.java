package com.consultorio.atencionclinica.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que registra una práctica odontológica efectivamente realizada
 * durante una consulta (acto médico).
 * Mapeada a la tabla `turno_practicas_realizadas` en db_atencion_clinica.
 *
 * DER definitivo (C4 / TPI):
 *   - turno_id      → Logical FK a turnos.id en db_agenda_usuarios
 *   - paciente_id   → FK física a pacientes.id (este microservicio)
 *   - practica_id   → FK física a practicas_odontologicas.id (este microservicio)
 *   - odontologo_id → Logical FK a usuarios.id en db_agenda_usuarios
 *
 * RN-10, RN-11: Solo se pueden registrar prácticas con activo = true.
 * RN-16: Permite verificar si un odontólogo atendió al paciente (para control de acceso
 *         a historia clínica confidencial sin consultar la tabla evoluciones_clinicas).
 *
 * Nota de diseño: esta tabla NO tiene FK a evoluciones_clinicas (a diferencia del
 * diseño anterior). El registro de práctica realizada está desacoplado de la nota
 * evolutiva para soportar el flujo F-06/F-08 del diagrama de actividades:
 * el odontólogo puede registrar las prácticas (F-08) y luego actualizar la evolución (F-07).
 */
@Entity
@Table(name = "turno_practicas_realizadas")
public class TurnoPracticaRealizada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Referencia lógica al turno en ms-agenda-usuarios donde se efectuó la práctica.
     * No existe FK física cruzada (patrón Database-per-Service).
     * El servicio valida su existencia y estado vía RestClient antes de registrar.
     */
    @Column(name = "turno_id", nullable = false)
    private Long turnoId;

    /**
     * FK física al paciente (mismo microservicio).
     * Redundancia deliberada para: búsquedas directas, RN-16 (control de acceso),
     * y consistencia de datos sin depender de joins inter-servicio.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tpr_paciente"))
    private Paciente paciente;

    /**
     * FK física a la práctica del nomenclador (mismo microservicio).
     * RN-10: La práctica debe estar activa (activo = true) al momento del registro.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practica_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tpr_practica"))
    private PracticaOdontologica practica;

    /**
     * Referencia lógica al odontólogo que realizó la práctica (en db_agenda_usuarios).
     * RN-16: Permite verificar si un odontólogo atendió alguna vez a un paciente.
     */
    @Column(name = "odontologo_id", nullable = false)
    private Long odontologoId;

    /**
     * Observación clínica libre: pieza tratada, anestesia utilizada, etc.
     */
    @Column(name = "nota_aclaratoria", columnDefinition = "TEXT")
    private String notaAclaratoria;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
    }

    // ── Constructores ────────────────────────────────────────────────────────

    public TurnoPracticaRealizada() {}

    // ── Getters y Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Long getTurnoId() { return turnoId; }
    public void setTurnoId(Long turnoId) { this.turnoId = turnoId; }

    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }

    public PracticaOdontologica getPractica() { return practica; }
    public void setPractica(PracticaOdontologica practica) { this.practica = practica; }

    public Long getOdontologoId() { return odontologoId; }
    public void setOdontologoId(Long odontologoId) { this.odontologoId = odontologoId; }

    public String getNotaAclaratoria() { return notaAclaratoria; }
    public void setNotaAclaratoria(String notaAclaratoria) { this.notaAclaratoria = notaAclaratoria; }

    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}
