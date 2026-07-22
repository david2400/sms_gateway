package com.essenza.sms_gateway.modules.SMSService.domain.model;

import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.routing.ProviderRoutingStrategy;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Configuración para proveedores de SMS
 * Soporta múltiples proveedores, failover y diferentes estrategias de enrutamiento
 */
@Data
@Builder
public class SmsProviderConfig {
    
    private SmsProvider provider;
    private String apiKey;
    private String apiSecret;
    private String baseUrl;
    private String username;
    private String password;
    private String sender;
    private Integer timeoutSeconds;
    private Integer maxRetries;
    private boolean enabled;
    private Map<String, Object> additionalProperties;
    
    // ========== Configuración de Routing ==========
    
    @Builder.Default
    private ProviderRoutingStrategy routingStrategy = ProviderRoutingStrategy.FAILOVER;
    
    @Builder.Default
    private List<SmsProvider> fallbackProviders = new ArrayList<>();
    
    @Builder.Default
    private boolean enableHealthCheck = true;
    
    @Builder.Default
    private int healthCheckIntervalSeconds = 60;
    
    /**
     * Obtiene una propiedad adicional
     */
    public Object getAdditionalProperty(String key) {
        return additionalProperties != null ? additionalProperties.get(key) : null;
    }
    
    /**
     * Agrega una propiedad adicional
     */
    public void setAdditionalProperty(String key, Object value) {
        if (additionalProperties == null) {
            additionalProperties = new java.util.HashMap<>();
        }
        additionalProperties.put(key, value);
    }
    
    /**
     * Verifica si la configuración es válida
     */
    public boolean isValid() {
        return provider != null && enabled &&
               ((apiKey != null && apiSecret != null) || 
                (username != null && password != null));
    }
}
