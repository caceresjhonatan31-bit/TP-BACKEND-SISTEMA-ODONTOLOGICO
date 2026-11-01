package com.consultorio.atencionclinica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.consultorio.atencionclinica",
        "com.consultorio.common"
})
public class MsAtencionClinicaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsAtencionClinicaApplication.class, args);
    }
}

