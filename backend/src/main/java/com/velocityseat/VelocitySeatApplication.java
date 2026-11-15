package com.velocityseat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VelocitySeatApplication {

    public static void main(String[] args) {
        SpringApplication.run(VelocitySeatApplication.class, args);
    }
}
