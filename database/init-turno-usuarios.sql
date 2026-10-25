-- ==========================================================
-- SCRIPT DE INICIALIZACIÓN: db_turno_usuarios (PostgreSQL)
-- ==========================================================

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    keycloak_id VARCHAR(100) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    dni VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    rol VARCHAR(30) NOT NULL,
    matricula VARCHAR(50) UNIQUE, -- Obligatorio solo para ODONTOLOGO
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usuario_rol CHECK (
        rol IN (
            'ADMIN',
            'ODONTOLOGO',
            'RECEPCIONISTA'
        )
    )
);

CREATE TABLE IF NOT EXISTS disponibilidad_odontologos (
    id BIGSERIAL PRIMARY KEY,
    odontologo_id BIGINT NOT NULL,
    dia_semana VARCHAR(15) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    CONSTRAINT fk_disponibilidad_odontologo FOREIGN KEY (odontologo_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT chk_dia_semana CHECK (
        dia_semana IN (
            'LUNES',
            'MARTES',
            'MIERCOLES',
            'JUEVES',
            'VIERNES',
            'SABADO',
            'DOMINGO'
        )
    ),
    CONSTRAINT chk_disponibilidad_horario CHECK (hora_fin > hora_inicio)
);

CREATE TABLE IF NOT EXISTS turnos (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL, -- Referencia lógica a pacientes.id (db_pacientes_practicas)
    odontologo_id BIGINT NOT NULL, -- FK física a usuarios.id
    creado_por_usuario_id BIGINT NOT NULL, -- FK física a usuarios.id (auditoría)
    practica_inicial_id BIGINT, -- Referencia lógica a practicas_odontologicas.id (db_pacientes_practicas)
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADO',
    nota_turno TEXT, -- Nota aclaratoria opcional al asignar el turno
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_turnos_odontologo FOREIGN KEY (odontologo_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT fk_turnos_creado_por FOREIGN KEY (creado_por_usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT chk_turno_estado CHECK (
        estado IN (
            'PROGRAMADO',
            'ATENDIDO',
            'INASISTIDO',
            'CANCELADO'
        )
    ),
    CONSTRAINT chk_turno_rango_horario CHECK (hora_fin > hora_inicio)
);

CREATE TABLE IF NOT EXISTS turno_practicas_realizadas (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT NOT NULL,
    practica_id BIGINT NOT NULL, -- Referencia lógica a practicas_odontologicas.id (db_pacientes_practicas)
    nota_aclaratoria TEXT, -- Nota opcional del odontólogo post-atención
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tpr_turno FOREIGN KEY (turno_id) REFERENCES turnos (id) ON DELETE CASCADE
);

-- Índices para búsquedas de personal, validación de superposición horaria e historial
CREATE INDEX idx_usuarios_rol_activo ON usuarios (rol, activo);

CREATE INDEX idx_disponibilidad_busqueda ON disponibilidad_odontologos (odontologo_id, dia_semana);

CREATE INDEX idx_turnos_validacion_superposicion ON turnos (
    odontologo_id,
    fecha,
    estado,
    hora_inicio,
    hora_fin
);

CREATE INDEX idx_turnos_historial_paciente ON turnos (paciente_id, id, fecha DESC);