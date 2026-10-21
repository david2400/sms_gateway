package com.essenza.sms_gateway.modules.SmsCore.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del módulo SmsCore ({@code sms.core.*} en application.yaml).
 */
@Data
@ConfigurationProperties(prefix = "sms.core")
public class SmsCoreProperties {

    /** Segundos que se cachean las rutas leídas de BD. 0 = sin caché. */
    private long routingCacheTtlSeconds = 60;

    /** Llave AES-256 (32 bytes en Base64) para {@code SmsCuenta.clave_cifrada}. Viene de SMS_CRYPTO_KEY; nunca en el repo. */
    private String cryptoKey;
}
