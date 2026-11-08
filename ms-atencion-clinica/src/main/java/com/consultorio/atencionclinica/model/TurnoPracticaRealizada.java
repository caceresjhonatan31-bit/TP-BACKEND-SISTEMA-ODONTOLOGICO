package com.consultorio.atencionclinica.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "turno_practicas_realizadas")
public class TurnoPracticaRealizada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turno_id", nullable = false)
    private Long turnoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tpr_paciente"))
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practica_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_tpr_practica"))
    private PracticaOdontologica practica;

    @Column(name = "odontologo_id", nullable = false)
    private Long odontologoId;

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
    public void setId(Long id) { this.id = id; }

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
