package com.essenza.sms_gateway.modules.SMSService.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades de configuración para el servicio externo de usuarios
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sms.usuario-service")
public class UsuarioServiceProperties {

    /**
     * URL base del servicio de usuarios (incluye puerto)
     */
    private String baseUrl;

    /**
     * Endpoint para guardar usuario
     */
    private String guardarEndpoint = "/api/login/usuarios/guardar";

    /**
     * Timeout de conexión en segundos
     */
    private Integer connectTimeoutSeconds = 30;

    /**
     * Timeout de lectura en segundos
     */
    private Integer readTimeoutSeconds = 30;

    /**
     * Habilitar el servicio
     */
    private Boolean enabled = true;

    /**
     * Reintentos máximos en caso de fallo
     */
    private Integer maxRetries = 3;

    /**
     * Obtiene la URL completa del endpoint de guardar
     */
    public String getGuardarUrl() {
        return baseUrl + guardarEndpoint;
    }
}
