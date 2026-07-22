package com.essenza.sms_gateway.modules.SMSService.infrastructure.config;

import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsProvider;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsProviderConfig;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.routing.ProviderRoutingStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuración centralizada de proveedores SMS desde application.yml
 * 
 * Ejemplo de configuración:
 * 
 * sms:
 *   routing:
 *     strategy: FAILOVER
 *     primary: inalambria
 *     fallback:
 *       - go4clients
 *       - twilio
 *     health-check:
 *       enabled: true
 *       interval-seconds: 60
 *   
 *   providers:
 *     inalambria:
 *       enabled: true
 *       base-url: https://api.inalambria.com
 *       username: ${INALAMBRIA_USER}
 *       password: ${INALAMBRIA_PASS}
 *       sender: "EMPRESA"
 *     
 *     go4clients:
 *       enabled: true
 *       base-url: https://api.go4clients.com
 *       api-key: ${GO4CLIENTS_KEY}
 *       api-secret: ${GO4CLIENTS_SECRET}
 *     
 *     twilio:
 *       enabled: false
 *       base-url: https://api.twilio.com
 *       api-key: ${TWILIO_SID}
 *       api-secret: ${TWILIO_TOKEN}
 */
@Data
@ConfigurationProperties(prefix = "sms")
public class SmsProvidersProperties {

    private RoutingProperties routing = new RoutingProperties();
    private Map<String, ProviderProperties> providers = new HashMap<>();

    @Data
    public static class RoutingProperties {
        private ProviderRoutingStrategy strategy = ProviderRoutingStrategy.FAILOVER;
        private String primary = "inalambria";
        private List<String> fallback = new ArrayList<>();
        private HealthCheckProperties healthCheck = new HealthCheckProperties();
    }

    @Data
    public static class HealthCheckProperties {
        private boolean enabled = true;
        private int intervalSeconds = 60;
    }

    @Data
    public static class ProviderProperties {
        private boolean enabled = false;
        private String baseUrl;
        private String apiKey;
        private String apiSecret;
        private String username;
        private String password;
        private String sender;
        private int timeoutSeconds = 30;
        private int maxRetries = 3;
        private Map<String, Object> extra = new HashMap<>();
    }

    /**
     * Obtiene la configuración del proveedor principal
     */
    public SmsProviderConfig getPrimaryConfig() {
        return getProviderConfig(routing.getPrimary());
    }

    /**
     * Obtiene la configuración de un proveedor específico
     */
    public SmsProviderConfig getProviderConfig(String providerCode) {
        ProviderProperties props = providers.get(providerCode);
        if (props == null) {
            return null;
        }

        return SmsProviderConfig.builder()
                .provider(SmsProvider.fromCode(providerCode))
                .enabled(props.isEnabled())
                .baseUrl(props.getBaseUrl())
                .apiKey(props.getApiKey())
                .apiSecret(props.getApiSecret())
                .username(props.getUsername())
                .password(props.getPassword())
                .sender(props.getSender())
                .timeoutSeconds(props.getTimeoutSeconds())
                .maxRetries(props.getMaxRetries())
                .additionalProperties(props.getExtra())
                .routingStrategy(routing.getStrategy())
                .fallbackProviders(getFallbackProvidersList())
                .enableHealthCheck(routing.getHealthCheck().isEnabled())
                .healthCheckIntervalSeconds(routing.getHealthCheck().getIntervalSeconds())
                .build();
    }

    /**
     * Obtiene la lista de proveedores de fallback
     */
    public List<SmsProvider> getFallbackProvidersList() {
        List<SmsProvider> result = new ArrayList<>();
        for (String code : routing.getFallback()) {
            try {
                result.add(SmsProvider.fromCode(code));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    /**
     * Obtiene todos los proveedores habilitados
     */
    public List<String> getEnabledProviders() {
        List<String> enabled = new ArrayList<>();
        for (Map.Entry<String, ProviderProperties> entry : providers.entrySet()) {
            if (entry.getValue().isEnabled()) {
                enabled.add(entry.getKey());
            }
        }
        return enabled;
    }

    /**
     * Verifica si un proveedor está habilitado
     */
    public boolean isProviderEnabled(String providerCode) {
        ProviderProperties props = providers.get(providerCode);
        return props != null && props.isEnabled();
    }

    @PostConstruct
    public void logConfiguration() {
        System.out.println("[SMS-CONFIG] Estrategia de routing: " + routing.getStrategy());
        System.out.println("[SMS-CONFIG] Proveedor principal: " + routing.getPrimary());
        System.out.println("[SMS-CONFIG] Proveedores fallback: " + routing.getFallback());
        System.out.println("[SMS-CONFIG] Proveedores habilitados: " + getEnabledProviders());
    }
}
