package com.consultorio.turnousuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
    "com.consultorio.turnousuarios",
    "com.consultorio.common"
})
@EnableScheduling
public class MsTurnoUsuariosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsTurnoUsuariosApplication.class, args);
    }
}
