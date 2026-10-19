package com.essenza.sms_gateway.modules.SmsCore.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del núcleo de SMS (orquestación multi-proveedor).
 * Las entidades y repositorios se registran en {@code config/MaestraConnectionConfig}.
 */
@Configuration
@EnableConfigurationProperties(SmsCoreProperties.class)
public class SmsCoreConfiguration {
}
