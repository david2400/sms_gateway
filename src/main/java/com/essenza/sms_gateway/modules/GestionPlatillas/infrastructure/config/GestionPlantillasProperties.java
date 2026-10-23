package com.essenza.sms_gateway.modules.GestionPlatillas.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuración del módulo de plantillas ({@code sms.plantillas.*}). */
@Data
@ConfigurationProperties(prefix = "sms.plantillas")
public class GestionPlantillasProperties {

    /** Segundos que se cachean las plantillas compiladas. 0 = sin caché. */
    private long cacheTtlSeconds = 300;
}
