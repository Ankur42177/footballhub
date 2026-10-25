package com.footballhub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI footballHubOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FootballHub API")
                .version("v1")
                .description("Premier League blogging and fan community. Browser clients authenticate with a "
                        + "session cookie; state-changing requests need the X-XSRF-TOKEN header copied from "
                        + "the XSRF-TOKEN cookie (GET /api/v1/auth/csrf sets it)."));
    }
}
