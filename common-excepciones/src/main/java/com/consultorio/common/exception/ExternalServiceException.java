package com.consultorio.common.exception;

// Para caídas o errores al comunicarse con la API de Feriados o con otro microservicio
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message) {
        super(message);
    }
}
