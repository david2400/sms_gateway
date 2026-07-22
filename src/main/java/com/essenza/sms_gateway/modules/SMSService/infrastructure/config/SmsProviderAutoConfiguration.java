package com.essenza.sms_gateway.modules.SMSService.infrastructure.config;

import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsProvider;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsProviderConfig;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Configuración automática de proveedores SMS
 * Se encarga de inicializar y configurar todos los proveedores disponibles
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SmsProviderAutoConfiguration {

    private final SmsProvidersProperties properties;
    private final List<SmsProviderPort> providers;

    @PostConstruct
    public void initializeProviders() {
        log.info("[SMS-AUTO-CONFIG] Inicializando {} proveedores SMS", providers.size());
        
        // Mostrar proveedores inyectados
        providers.forEach(provider -> {
            log.info("[SMS-AUTO-CONFIG] Provider inyectado: {} ({})", 
                    provider.getProviderName(), provider.getProviderType());
        });

        // Configurar cada proveedor si está habilitado
        properties.getEnabledProviders().forEach(providerCode -> {
            try {
                SmsProvider providerType = SmsProvider.fromCode(providerCode);
                SmsProviderConfig config = properties.getProviderConfig(providerCode);
                
                if (config != null && config.isValid()) {
                    // Buscar el provider correspondiente y configurarlo
                    providers.stream()
                            .filter(p -> p.getProviderType() == providerType)
                            .findFirst()
                            .ifPresent(provider -> {
                                provider.configure(config);
                                log.info("[SMS-AUTO-CONFIG] ✅ {} configurado y disponible: {}", 
                                        providerCode, provider.isAvailable());
                            });
                } else {
                    log.warn("[SMS-AUTO-CONFIG] ❌ Configuración inválida para provider: {}", providerCode);
                }
            } catch (Exception e) {
                log.error("[SMS-AUTO-CONFIG] Error configurando provider {}: {}", providerCode, e.getMessage());
            }
        });
    }
}
