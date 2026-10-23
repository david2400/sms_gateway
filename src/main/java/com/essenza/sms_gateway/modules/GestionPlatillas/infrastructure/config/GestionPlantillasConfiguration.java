package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Módulo GestionPlatillas: plantillas SMS con variables (administración, consulta y SPI de SmsCore). */
@Configuration
@EnableConfigurationProperties(GestionPlantillasProperties.class)
public class GestionPlantillasConfiguration {
}
