-- ==========================================================
-- SCRIPT DE INICIALIZACIÓN: db_atencion_clinica (PostgreSQL)
-- Microservicio 2: ms-atencion-clinica (Atención Clínica)
-- Bounded Context: Acto médico, historia clínica, nomenclador
-- DER: Versión definitiva alineada con diagramas C4 y TPI
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
    codigo_nomenclador VARCHAR(20) NOT NULL UNIQUE,  -- DER definitivo: varchar(20)
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS evoluciones_clinicas (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL,                     -- FK fisica a pacientes.id (mismo microservicio)
    turno_desde_id BIGINT NOT NULL,                  -- Logical FK a turnos.id en db_agenda_usuarios
    turno_hasta_id BIGINT,                           -- Logical FK nullable (RN-15: sesion unica o tratamiento en curso)
    actualizado_por_odontologo_id BIGINT NOT NULL,   -- Logical FK a usuarios.id en db_agenda_usuarios
    diagnostico VARCHAR(255) NOT NULL,
    evolucion_texto TEXT NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evolucion_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id) ON DELETE CASCADE,
    CONSTRAINT chk_rango_turnos CHECK (
        turno_hasta_id IS NULL
        OR turno_hasta_id >= turno_desde_id
    )
);

CREATE TABLE IF NOT EXISTS turno_practicas_realizadas (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT NOT NULL,                        -- Logical FK a turnos.id en db_agenda_usuarios
    paciente_id BIGINT NOT NULL,                     -- FK fisica a pacientes.id
    practica_id BIGINT NOT NULL,                     -- FK fisica a practicas_odontologicas.id
    odontologo_id BIGINT NOT NULL,                   -- Logical FK a usuarios.id en db_agenda_usuarios
    nota_aclaratoria TEXT,
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tpr_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes (id) ON DELETE RESTRICT,
    CONSTRAINT fk_tpr_practica FOREIGN KEY (practica_id) REFERENCES practicas_odontologicas (id) ON DELETE RESTRICT
);

-- ======================================================
-- Indices para busqueda de pacientes, nomenclador y actos
-- ======================================================
CREATE INDEX idx_pacientes_dni ON pacientes (dni);
CREATE INDEX idx_pacientes_apellido_nombre ON pacientes (apellido, nombre);

CREATE INDEX idx_practicas_codigo ON practicas_odontologicas (codigo_nomenclador);

CREATE INDEX idx_evoluciones_paciente ON evoluciones_clinicas (paciente_id, turno_desde_id);
CREATE INDEX idx_evoluciones_odontologo ON evoluciones_clinicas (actualizado_por_odontologo_id);

CREATE INDEX idx_tpr_turno ON turno_practicas_realizadas (turno_id);
CREATE INDEX idx_tpr_paciente ON turno_practicas_realizadas (paciente_id);
CREATE INDEX idx_tpr_practica ON turno_practicas_realizadas (practica_id);
CREATE INDEX idx_tpr_odontologo ON turno_practicas_realizadas (odontologo_id);
