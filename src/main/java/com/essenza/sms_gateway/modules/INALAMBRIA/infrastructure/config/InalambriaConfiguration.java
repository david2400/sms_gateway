package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registra {@link InalambriaProperties}. */
@Configuration
@EnableConfigurationProperties(InalambriaProperties.class)
public class InalambriaConfiguration {
}
