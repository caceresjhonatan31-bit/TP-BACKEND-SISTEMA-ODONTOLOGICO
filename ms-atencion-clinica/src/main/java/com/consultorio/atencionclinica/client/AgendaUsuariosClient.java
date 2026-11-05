package com.consultorio.atencionclinica.client;

import com.consultorio.atencionclinica.dto.response.TurnoExternalDTO;
import com.consultorio.common.exception.ExternalServiceException;
import com.consultorio.common.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Cliente HTTP para comunicarse con ms-agenda-usuarios.
 *
 * Patrón C4 Nivel 2: ms-atencion-clinica consulta a ms-agenda-usuarios
 * via REST para validar el estado del turno y el profesional interviniente
 * antes de registrar prácticas o evoluciones clínicas.
 *
 * Manejo de errores:
 *   - HTTP 404 → ResourceNotFoundException (el turno no existe en agenda).
 *   - HTTP 5xx / error de red → ExternalServiceException (servicio no disponible).
 */
@Component
public class AgendaUsuariosClient {

    private final RestClient restClient;

    public AgendaUsuariosClient(@Qualifier("agendaUsuariosRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Consulta los datos de un turno en ms-agenda-usuarios.
     *
     * @param turnoId ID del turno a verificar.
     * @return {@link TurnoExternalDTO} con id, pacienteId, odontologoId y estado.
     * @throws ResourceNotFoundException si el turno no existe (404).
     * @throws ExternalServiceException  si ms-agenda-usuarios no responde o devuelve 5xx.
     */
    public TurnoExternalDTO obtenerTurno(Long turnoId) {
        try {
            return restClient.get()
                    .uri("/api/turnos/{id}", turnoId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        throw new ResourceNotFoundException(
                                "El turno con ID " + turnoId + " no existe en ms-agenda-usuarios.");
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new ExternalServiceException(
                                "ms-agenda-usuarios no está disponible. Intente nuevamente más tarde.");
                    })
                    .body(TurnoExternalDTO.class);

        } catch (ResourceNotFoundException | ExternalServiceException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new ExternalServiceException(
                    "Error al comunicarse con ms-agenda-usuarios: " + e.getMessage());
        } catch (Exception e) {
            throw new ExternalServiceException(
                    "No se pudo conectar con ms-agenda-usuarios. Verifique que el servicio esté activo.");
        }
    }
}
