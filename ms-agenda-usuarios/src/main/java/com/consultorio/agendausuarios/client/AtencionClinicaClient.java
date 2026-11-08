package com.consultorio.agendausuarios.client;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.consultorio.common.exception.ConflictException;
import com.consultorio.common.exception.ExternalServiceException;
import com.consultorio.common.exception.ResourceNotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AtencionClinicaClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final boolean enabled;

    public AtencionClinicaClient(
            RestClient.Builder restClientBuilder,
            @Value("${clients.atencion-clinica.url:http://localhost:8082}") String baseUrl,
            @Value("${clients.atencion-clinica.enabled:false}") boolean enabled) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
        this.enabled = enabled;
    }

    /**
     * RN-06: Comprueba en ms-atencion-clinica que el paciente exista y esté activo,
     * y que la práctica odontológica inicial exista y se encuentre vigente.
     */
    public void validarVigenciaPacienteYPractica(Long pacienteId, Long practicaId) {
        if (!enabled) {
            log.debug("[AtencionClinicaClient] Validación inter-servicio deshabilitada por configuración (clients.atencion-clinica.enabled=false).");
            return;
        }

        validarPacienteActivo(pacienteId);
        validarPracticaVigente(practicaId);
    }

    private void validarPacienteActivo(Long pacienteId) {
        try {
            Map<String, Object> paciente = restClient.get()
                    .uri(baseUrl + "/api/pacientes/{id}", pacienteId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (paciente == null) {
                throw new ResourceNotFoundException("Paciente no encontrado con ID: " + pacienteId + " (RN-06)");
            }

            Object activoObj = paciente.get("activo");
            if (Boolean.FALSE.equals(activoObj)) {
                throw new ConflictException("El paciente con ID " + pacienteId + " se encuentra inactivo en el padrón; debe reactivarse antes de asignarle un turno (RN-06 / RN-13)");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("El paciente con ID " + pacienteId + " no existe en el padrón (RN-06)");
            }
            throw new ExternalServiceException("Error al validar vigencia del paciente en ms-atencion-clinica: " + e.getMessage());
        } catch (ResourceNotFoundException | ConflictException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("No se pudo conectar con ms-atencion-clinica para validar el paciente: " + e.getMessage());
        }
    }

    private void validarPracticaVigente(Long practicaId) {
        try {
            Map<String, Object> practica = restClient.get()
                    .uri(baseUrl + "/api/practicas/{id}", practicaId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (practica == null) {
                throw new ResourceNotFoundException("Práctica odontológica no encontrada con ID: " + practicaId + " (RN-06)");
            }

            Object activoObj = practica.get("activo");
            if (Boolean.FALSE.equals(activoObj)) {
                throw new ConflictException("La práctica odontológica con ID " + practicaId + " se encuentra inhabilitada en el catálogo oficial (RN-06 / RN-14)");
            }
        } catch (HttpClientErrorException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 404) {
                throw new ResourceNotFoundException("La práctica odontológica con ID " + practicaId + " no existe en el catálogo (RN-06)");
            }
            throw new ExternalServiceException("Error al validar vigencia de la práctica en ms-atencion-clinica: " + e.getMessage());
        } catch (ResourceNotFoundException | ConflictException e) {
            throw e;
        } catch (Exception e) {
            throw new ExternalServiceException("No se pudo conectar con ms-atencion-clinica para validar la práctica: " + e.getMessage());
        }
    }
}

