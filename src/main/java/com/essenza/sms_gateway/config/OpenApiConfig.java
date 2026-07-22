package com.essenza.sms_gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de OpenAPI para Swagger UI
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SMS Gateway API")
                        .version("1.0.0")
                        .description("API para envío y gestión de SMS con múltiples proveedores")
                        .contact(new Contact()
                                .name("Essenza")
                                .email("soporte@essenza.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }

    @Bean
    public GroupedOpenApi otpServiceApi() {
        return GroupedOpenApi.builder()
                .group("otp-service")
                .packagesToScan("com.essenza.sms_gateway.modules.SMSService.infrastructure.inbound.rest")
                .pathsToMatch("/api/sms_gateway/otp/**")
                .build();
    }

    @Bean
    public GroupedOpenApi smsServiceApi() {
        return GroupedOpenApi.builder()
                .group("sms-service")
                .packagesToScan("com.essenza.sms_gateway.modules.SMSService.infrastructure.inbound.rest")
                .pathsToMatch("/api/sms_gateway/sms/**")
                .build();
    }

    @Bean
    public GroupedOpenApi smsCampaignServiceApi() {
        return GroupedOpenApi.builder()
                .group("sms-campaign-service")
                .packagesToScan("com.essenza.sms_gateway.modules.SMSService.infrastructure.inbound.rest")
                .pathsToMatch("/api/sms_gateway/campaign/**")
                .build();
    }
}
