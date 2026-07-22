package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.routing;

import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsProvider;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Estado de salud de un proveedor SMS
 */
@Data
@Builder
public class ProviderHealthStatus {
    
    private SmsProvider provider;
    private boolean available;
    private int successCount;
    private int failureCount;
    private long averageResponseTimeMs;
    private Instant lastCheck;
    private Instant lastSuccess;
    private Instant lastFailure;
    private String lastErrorMessage;
    
    /**
     * Calcula el porcentaje de éxito
     */
    public double getSuccessRate() {
        int total = successCount + failureCount;
        return total > 0 ? (double) successCount / total * 100 : 0;
    }
    
    /**
     * Determina si el proveedor está saludable
     */
    public boolean isHealthy() {
        int total = successCount + failureCount;
        // Si no hay estadísticas aún (estado inicial), considerar saludable si está disponible
        if (total == 0) {
            return available;
        }
        // Con estadísticas, requiere tasa de éxito >= 50%
        return available && getSuccessRate() >= 50;
    }
    
    /**
     * Incrementa contador de éxitos
     */
    public void recordSuccess(long responseTimeMs) {
        this.successCount++;
        this.lastSuccess = Instant.now();
        this.averageResponseTimeMs = (this.averageResponseTimeMs + responseTimeMs) / 2;
    }
    
    /**
     * Incrementa contador de fallos
     */
    public void recordFailure(String errorMessage) {
        this.failureCount++;
        this.lastFailure = Instant.now();
        this.lastErrorMessage = errorMessage;
    }
    
    /**
     * Resetea estadísticas
     */
    public void resetStats() {
        this.successCount = 0;
        this.failureCount = 0;
        this.averageResponseTimeMs = 0;
        this.lastErrorMessage = null;
    }
}
