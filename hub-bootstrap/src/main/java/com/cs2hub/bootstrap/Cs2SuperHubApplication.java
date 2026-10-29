package com.cs2hub.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.cs2hub")
public class Cs2SuperHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(Cs2SuperHubApplication.class, args);
    }
}
