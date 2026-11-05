package com.consultorio.atencionclinica.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuración del cliente HTTP hacia ms-agenda-usuarios.
 *
 * Usa Spring 6 RestClient (sin Feign) para mantener dependencias mínimas.
 * La URL base se inyecta desde application.yml: clients.agenda-usuarios.url
 */
@Configuration
public class RestClientConfig {

    @Value("${clients.agenda-usuarios.url}")
    private String agendaUsuariosUrl;

    /**
     * Bean RestClient configurado con la URL base de ms-agenda-usuarios.
     * Se inyecta en AgendaUsuariosClient via constructor.
     */
    @Bean
    public RestClient agendaUsuariosRestClient() {
        return RestClient.builder()
                .baseUrl(agendaUsuariosUrl)
                .build();
    }
}
