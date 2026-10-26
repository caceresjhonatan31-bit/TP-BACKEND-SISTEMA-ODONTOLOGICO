package com.consultorio.pacientespracticas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.consultorio.pacientespracticas",
        "com.consultorio.common"
})
public class MsPacientesPracticasApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsPacientesPracticasApplication.class, args);
    }
}