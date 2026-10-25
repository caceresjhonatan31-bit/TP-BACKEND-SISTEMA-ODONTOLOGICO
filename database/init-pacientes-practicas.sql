-- ==========================================================
-- SCRIPT DE INICIALIZACIÓN: db_pacientes_practicas (PostgreSQL)
-- ==========================================================

CREATE TABLE IF NOT EXISTS pacientes (
    id BIGSERIAL PRIMARY KEY,
    dni VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    email VARCHAR(150),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_alta TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evoluciones_clinicas (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL, -- Relación 1:N (un paciente puede tener varias evoluciones)
    turno_desde_id BIGINT NOT NULL, -- Referencia lógica al turno inicial en db_turno_usuarios
    turno_hasta_id BIGINT, -- Referencia lógica al turno final (NULL si sigue en curso)
    diagnostico VARCHAR(255) NOT NULL, -- Diagnóstico asociado a ese rango de turnos
    evolucion_texto TEXT NOT NULL, -- Notas clínicas y evolución en formato texto
    ultima_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_por_odontologo_id BIGINT NOT NULL, -- Referencia lógica a usuarios.id (db_turno_usuarios)
    CONSTRAINT fk_evolucion_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id) ON DELETE CASCADE,
    CONSTRAINT chk_rango_turnos CHECK (
        turno_hasta_id IS NULL
        OR turno_hasta_id >= turno_desde_id
    )
);

CREATE TABLE IF NOT EXISTS practicas_odontologicas (
    id BIGSERIAL PRIMARY KEY,
    codigo_nomenclador VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Índices para búsqueda rápida en recepción, historial evolutivo y nomenclador
CREATE INDEX idx_pacientes_dni ON pacientes (dni);

CREATE INDEX idx_pacientes_apellido_nombre ON pacientes (apellido, nombre);

CREATE INDEX idx_evoluciones_paciente ON evoluciones_clinicas (paciente_id, turno_desde_id);

CREATE INDEX idx_practicas_codigo ON practicas_odontologicas (codigo_nomenclador);