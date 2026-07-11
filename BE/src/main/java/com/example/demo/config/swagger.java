package com.example.demo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SwaggerConfig {

    @Bean
    public OpenAPI healthAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Health Management API")
                        .description("API Documentation for Health Management System")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("VHTL Team")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}