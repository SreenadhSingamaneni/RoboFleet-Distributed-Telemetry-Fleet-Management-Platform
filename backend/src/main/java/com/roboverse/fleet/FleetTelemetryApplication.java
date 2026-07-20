package com.roboverse.fleet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class FleetTelemetryApplication {

    public static void main(String[] args) {
        SpringApplication.run(FleetTelemetryApplication.class, args);
    }
}

