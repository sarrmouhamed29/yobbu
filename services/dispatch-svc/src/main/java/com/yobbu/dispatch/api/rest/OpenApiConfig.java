package com.yobbu.dispatch.api.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dispatchServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Yóbbu — Dispatch Service API")
                        .version("v1")
                        .description("Courier management and delivery dispatch.")
                        .license(new License().name("MIT")));
    }
}
