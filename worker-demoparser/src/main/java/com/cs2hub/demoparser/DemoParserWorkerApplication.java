package com.cs2hub.demoparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DemoParserWorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoParserWorkerApplication.class, args);
    }
}
