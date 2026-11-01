package com.consultorio.agendausuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
    "com.consultorio.agendausuarios",
    "com.consultorio.common"
})
@EnableScheduling
public class MsAgendaUsuariosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsAgendaUsuariosApplication.class, args);
    }
}

