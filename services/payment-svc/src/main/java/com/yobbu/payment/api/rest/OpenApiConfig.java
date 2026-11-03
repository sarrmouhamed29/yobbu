package com.yobbu.payment.api.rest;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Yóbbu — Payment Service API")
                        .version("v1")
                        .description(
                                "Simulated mobile-money payment. No real money is processed. " +
                                "Idempotent: same idempotency key always returns the same payment.")
                        .license(new License().name("MIT")));
    }
}
