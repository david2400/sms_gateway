package com.essenza.sms_gateway.modules.SMSService.infrastructure.config;

import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsProvider;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsProviderConfig;
import com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.routing.SmsProviderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import jakarta.annotation.PostConstruct;

/**
 * Configuración específica para el SmsProviderRouter
 * Asegura que el router tenga la configuración correcta al iniciar
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SmsProviderRouterConfig {

    private final SmsProvidersProperties properties;
    private final SmsProviderRouter router;

    @PostConstruct
    public void validateConfiguration() {
        log.info("[SMS-ROUTER-CONFIG] Validando configuración del router SMS");
        
        // Validar que haya proveedores habilitados
        if (properties.getEnabledProviders().isEmpty()) {
            log.warn("[SMS-ROUTER-CONFIG] ⚠️ No hay proveedores SMS habilitados");
            return;
        }
        
        log.info("[SMS-ROUTER-CONFIG] ✅ Proveedores habilitados: {}", properties.getEnabledProviders());
        log.info("[SMS-ROUTER-CONFIG] ✅ Estrategia: {}", properties.getRouting().getStrategy());
        log.info("[SMS-ROUTER-CONFIG] ✅ Principal: {}", properties.getRouting().getPrimary());
        log.info("[SMS-ROUTER-CONFIG] ✅ Fallback: {}", properties.getRouting().getFallback());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[SMS-ROUTER-CONFIG] Aplicación lista, verificando estado del router");
        
        // Obtener estadísticas del router
        var stats = router.getStats();
        log.info("[SMS-ROUTER-CONFIG] 📊 Estadísticas del router:");
        log.info("[SMS-ROUTER-CONFIG]   - Estrategia actual: {}", stats.getCurrentStrategy());
        log.info("[SMS-ROUTER-CONFIG]   - Proveedor principal: {}", stats.getPrimaryProvider());
        log.info("[SMS-ROUTER-CONFIG]   - Proveedores registrados: {}", stats.getRegisteredProviders());
        log.info("[SMS-ROUTER-CONFIG]   - Proveedores disponibles: {}", stats.getAvailableProviders());
        
        // Si no hay proveedores disponibles, mostrar advertencia
        if (stats.getAvailableProviders() == 0) {
            log.error("[SMS-ROUTER-CONFIG] ❌ CRÍTICO: No hay proveedores SMS disponibles!");
            log.error("[SMS-ROUTER-CONFIG]    Verifique la configuración de los providers en application.properties");
            
            // Mostrar estados de salud para diagnóstico
            if (stats.getHealthStatuses() != null) {
                stats.getHealthStatuses().forEach((provider, status) -> {
                    log.error("[SMS-ROUTER-CONFIG]    - {}: disponible={}, saludable={}", 
                            provider.getName(), status.isAvailable(), status.isHealthy());
                });
            }
        } else {
            log.info("[SMS-ROUTER-CONFIG] ✅ Router SMS listo para operar");
        }
    }
}
