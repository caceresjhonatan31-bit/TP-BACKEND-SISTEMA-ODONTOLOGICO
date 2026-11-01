-- ==========================================================
-- SCRIPT DE INICIALIZACIÓN: db_atencion_clinica (PostgreSQL)
-- Microservicio 2: ms-atencion-clinica (Atención Clínica)
-- Agrupa el acto médico: pacientes, evoluciones, prácticas
-- y prácticas efectivamente realizadas vinculadas a la evolución
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

CREATE TABLE IF NOT EXISTS practicas_odontologicas (
    id BIGSERIAL PRIMARY KEY,
    codigo_nomenclador VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS evoluciones_clinicas (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL, -- Relación 1:N (un paciente posee múltiples evoluciones clínicas)
    turno_desde_id BIGINT NOT NULL, -- Referencia lógica al turno inicial en db_agenda_usuarios
    turno_hasta_id BIGINT, -- Referencia lógica al turno final (NULL si corresponde a una sola sesión o sigue en curso)
    diagnostico VARCHAR(255) NOT NULL, -- Diagnóstico clínico del acto médico
    evolucion_texto TEXT NOT NULL, -- Notas clínicas y evolución en formato texto
    ultima_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_por_odontologo_id BIGINT NOT NULL, -- Referencia lógica al odontólogo en db_agenda_usuarios (usuarios.id)
    CONSTRAINT fk_evolucion_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id) ON DELETE CASCADE,
    CONSTRAINT chk_rango_turnos CHECK (
        turno_hasta_id IS NULL
        OR turno_hasta_id >= turno_desde_id
    )
);

CREATE TABLE IF NOT EXISTS turno_practicas_realizadas (
    id BIGSERIAL PRIMARY KEY,
    evolucion_id BIGINT NOT NULL, -- Vinculación directa con la evolución clínica del acto médico
    practica_id BIGINT NOT NULL, -- FK física al nomenclador de prácticas dentro de este mismo microservicio
    turno_id BIGINT NOT NULL, -- Referencia lógica al turno en db_agenda_usuarios donde se efectuó
    nota_aclaratoria TEXT, -- Nota u observación clínica post-atención (ej: pieza tratada, anestesia utilizada)
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tpr_evolucion FOREIGN KEY (evolucion_id) REFERENCES evoluciones_clinicas (id) ON DELETE CASCADE,
    CONSTRAINT fk_tpr_practica FOREIGN KEY (practica_id) REFERENCES practicas_odontologicas (id) ON DELETE RESTRICT
);

-- Índices para búsqueda de pacientes, catálogo de nomenclador e historia clínica
CREATE INDEX idx_pacientes_dni ON pacientes (dni);

CREATE INDEX idx_pacientes_apellido_nombre ON pacientes (apellido, nombre);

CREATE INDEX idx_practicas_codigo ON practicas_odontologicas (codigo_nomenclador);

CREATE INDEX idx_evoluciones_paciente ON evoluciones_clinicas (paciente_id, turno_desde_id);

CREATE INDEX idx_tpr_evolucion ON turno_practicas_realizadas (evolucion_id);

CREATE INDEX idx_tpr_turno ON turno_practicas_realizadas (turno_id);

CREATE INDEX idx_tpr_practica ON turno_practicas_realizadas (practica_id);

