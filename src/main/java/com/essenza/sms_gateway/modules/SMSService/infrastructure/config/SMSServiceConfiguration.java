package com.essenza.sms_gateway.modules.SMSService.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuración del módulo SMSService
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({SmsProvidersProperties.class})
@ComponentScan(basePackages = {
    "com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers",
    "com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.adapters",
    "com.essenza.sms_gateway.modules.SMSService.application.services",
    "com.essenza.sms_gateway.modules.SMSService.infrastructure.config"
})
@Import({SmsHttpClientConfig.class})
public class SMSServiceConfiguration {
    
    public SMSServiceConfiguration() {
        log.info("[SMSService] Module initialized");
    }
}
