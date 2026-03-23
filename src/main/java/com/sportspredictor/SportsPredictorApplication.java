package com.sportspredictor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;

/** Sports Predictor MCP server application entry point. */
@SpringBootApplication(exclude = {CacheAutoConfiguration.class})
public class SportsPredictorApplication {

    /** Starts the Spring Boot application. */
    public static void main(String[] args) {
        SpringApplication.run(SportsPredictorApplication.class, args);
    }
}
