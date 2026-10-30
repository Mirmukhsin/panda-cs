package com.cs2hub.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EntityScan(basePackages = "com.cs2hub")
@EnableJpaRepositories(basePackages = "com.cs2hub")
@SpringBootApplication(scanBasePackages = "com.cs2hub")
public class Cs2SuperHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(Cs2SuperHubApplication.class, args);
    }
}
