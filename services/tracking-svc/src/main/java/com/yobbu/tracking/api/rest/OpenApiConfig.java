package com.yobbu.tracking.api.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI trackingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Yóbbu — Tracking Service API")
                        .version("v1")
                        .description("Real-time courier position tracking.")
                        .license(new License().name("MIT")));
    }
}
