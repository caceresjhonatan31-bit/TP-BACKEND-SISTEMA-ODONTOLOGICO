package com.consultorio.agendausuarios.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.consultorio.agendausuarios.domain.enums.EstadoTurno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "turnos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "odontologo_id", nullable = false)
    private Usuario odontologo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_usuario_id")
    private Usuario creadoPorUsuario;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "practica_inicial_id")
    private Long practicaInicialId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTurno estado = EstadoTurno.PROGRAMADO;

    @Column(name = "nota_aclaratoria", columnDefinition = "TEXT")
    private String notaAclaratoria;

    @Column(name = "google_event_id", length = 150)
    private String googleEventId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoTurno.PROGRAMADO;
        }
    }
}
