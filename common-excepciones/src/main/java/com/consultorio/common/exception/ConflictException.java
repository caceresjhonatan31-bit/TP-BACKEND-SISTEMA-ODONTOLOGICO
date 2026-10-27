package com.consultorio.common.exception;

// Para choques de reglas de negocio (ej. TurnoOcupadoException, DiaFeriadoException, DniDuplicado)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
