package com.footballhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FootballHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(FootballHubApplication.class, args);
    }
}
