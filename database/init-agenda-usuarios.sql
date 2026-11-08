-- ==========================================================
-- SCRIPT DE INICIALIZACIÓN: db_agenda_usuarios (PostgreSQL)
-- Microservicio 1: ms-agenda-usuarios (Agenda Operativa)
-- Administra personal, disponibilidad y reserva horaria
-- ==========================================================

CREATE TABLE IF NOT EXISTS usuarios (
    id BIGSERIAL PRIMARY KEY,
    keycloak_id VARCHAR(100) UNIQUE, -- Permite NULL temporalmente hasta integrar Keycloak
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
    activo BOOLEAN NOT NULL DEFAULT TRUE,
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
    paciente_id BIGINT NOT NULL, -- Referencia lógica a pacientes.id (db_atencion_clinica)
    odontologo_id BIGINT NOT NULL, -- FK física a usuarios.id
    creado_por_usuario_id BIGINT, -- Permite NULL temporalmente hasta integrar autenticación JWT
    practica_inicial_id BIGINT, -- Referencia lógica a practicas_odontologicas.id (db_atencion_clinica)
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADO',
    nota_aclaratoria TEXT, -- Nota u observación opcional al solicitar/asignar el turno
    google_event_id VARCHAR(150), -- Identificador del evento en Google Calendar API (RN-04)
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

-- Datos iniciales de prueba
INSERT INTO
    usuarios (
        dni,
        nombre,
        apellido,
        email,
        rol,
        matricula,
        activo
    )
VALUES (
        '20111222',
        'Carlos',
        'Gomez',
        'admin@consultorio.com',
        'ADMIN',
        NULL,
        TRUE
    ),
    (
        '30222333',
        'Laura',
        'Perez',
        'recepcion@consultorio.com',
        'RECEPCIONISTA',
        NULL,
        TRUE
    ),
    (
        '25333444',
        'Martin',
        'Lopez',
        'mlopez@consultorio.com',
        'ODONTOLOGO',
        'MP-1045',
        TRUE
    ),
    (
        '28444555',
        'Sofia',
        'Martinez',
        'smartinez@consultorio.com',
        'ODONTOLOGO',
        'MP-2089',
        TRUE
    )
ON CONFLICT (dni) DO NOTHING;

INSERT INTO
    disponibilidad_odontologos (
        odontologo_id,
        dia_semana,
        hora_inicio,
        hora_fin,
        activo
    )
VALUES (
        3,
        'LUNES',
        '08:00:00',
        '12:00:00',
        TRUE
    ),
    (
        3,
        'MIERCOLES',
        '14:00:00',
        '18:00:00',
        TRUE
    ),
    (
        3,
        'VIERNES',
        '08:00:00',
        '12:00:00',
        TRUE
    );