package com.consultorio.atencionclinica.client;

import com.consultorio.atencionclinica.dto.response.TurnoExternalDTO;
import com.consultorio.common.exception.ExternalServiceException;
import com.consultorio.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class AgendaUsuariosClient {

    private final RestClient restClient;

    public AgendaUsuariosClient(@Qualifier("agendaUsuariosRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @param turnoId ID del turno a verificar.
     * @return {@link TurnoExternalDTO} con id, pacienteId, odontologoId y estado.
     * @throws ResourceNotFoundException si el turno no existe (404).
     * @throws ExternalServiceException  si ms-agenda-usuarios no responde o devuelve 5xx.
     */
    public TurnoExternalDTO obtenerTurno(Long turnoId) {
        log.debug("Consultando estado de turno ID {} en ms-agenda-usuarios", turnoId);
        try {
            return restClient.get()
                    .uri("/api/turnos/{id}", turnoId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        log.warn("Turno con ID {} no encontrado en ms-agenda-usuarios (4xx)", turnoId);
                        throw new ResourceNotFoundException(
                                "El turno con ID " + turnoId + " no existe en ms-agenda-usuarios.");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        log.error("Fallo del servidor ms-agenda-usuarios al consultar turno ID {} (5xx)", turnoId);
                        throw new ExternalServiceException(
                                "ms-agenda-usuarios no está disponible. Intente nuevamente más tarde.");
                    })
                    .body(TurnoExternalDTO.class);

        } catch (ResourceNotFoundException | ExternalServiceException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("Error HTTP al comunicarse con ms-agenda-usuarios para turno ID {}: {}", turnoId, e.getMessage());
            throw new ExternalServiceException(
                    "Error al comunicarse con ms-agenda-usuarios: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error inesperado de conexión con ms-agenda-usuarios para turno ID {}: {}", turnoId, e.getMessage());
            throw new ExternalServiceException(
                    "No se pudo conectar con ms-agenda-usuarios. Verifique que el servicio esté activo.");
        }
    }
}
