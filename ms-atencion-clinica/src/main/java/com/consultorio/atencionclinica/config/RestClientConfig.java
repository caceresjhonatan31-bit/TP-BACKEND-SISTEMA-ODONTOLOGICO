package com.consultorio.atencionclinica.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


@Configuration
public class RestClientConfig {

    @Value("${clients.agenda-usuarios.url}")
    private String agendaUsuariosUrl;

    @Bean
    public RestClient agendaUsuariosRestClient() {
        return RestClient.builder()
                .baseUrl(agendaUsuariosUrl)
                .build();
    }
}
