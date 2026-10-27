package com.consultorio.common.exception;

// Para recursos inexistentes (ej. PacienteNoEncontrado, PracticaNoEncontrada, TurnoNoEncontrado)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
