package com.yobbu.order.api.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Yóbbu — Order Service API")
                        .version("v1")
                        .description("Order lifecycle management: create, track and transition orders.")
                        .license(new License().name("MIT")));
    }
}
