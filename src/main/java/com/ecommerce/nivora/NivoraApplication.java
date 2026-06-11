package com.ecommerce.nivora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NivoraApplication {
    public static void main(String[] args) {
        SpringApplication.run(NivoraApplication.class, args);
    }
}
