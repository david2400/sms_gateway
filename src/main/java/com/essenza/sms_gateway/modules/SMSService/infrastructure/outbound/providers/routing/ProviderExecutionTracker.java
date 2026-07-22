package com.essenza.sms_gateway.modules.SMSService.infrastructure.outbound.providers.routing;

import com.essenza.sms_gateway.modules.SMSService.domain.dto.Otp.OtpSendResult;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsBatchResult;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsCampaignOperationResult;
import com.essenza.sms_gateway.modules.SMSService.domain.model.SmsProvider;
import com.essenza.sms_gateway.modules.SMSService.domain.ports.SmsProviderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * Responsable únicamente de EJECUTAR una operación contra un proveedor ya
 * resuelto, midiendo tiempos de respuesta y registrando éxito/fallo en el
 * {@link SmsProviderRegistry} (health tracking). No decide qué proveedor usar
 * ni qué hacer si falla (eso es responsabilidad de {@code SmsProviderRouter}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderExecutionTracker {

    private final SmsProviderRegistry registry;

    public <T> T executeWithTracking(SmsProviderPort provider, Function<SmsProviderPort, T> operation,
                                      Function<String, T> errorHandler) {
        long startTime = System.currentTimeMillis();
        SmsProvider type = provider.getProviderType();

        try {
            T result = operation.apply(provider);
            long responseTime = System.currentTimeMillis() - startTime;

            if (isSuccessfulResult(result)) {
                registry.recordSuccess(type, responseTime);
                log.info("[TRACKER] ✅ {} respondió exitosamente en {}ms", provider.getProviderName(), responseTime);
            } else {
                registry.recordFailure(type, "Resultado no exitoso");
            }

            return result;
        } catch (Exception e) {
            registry.recordFailure(type, e.getMessage());
            log.error("[TRACKER] ❌ Error en {}: {}", provider.getProviderName(), e.getMessage());
            return errorHandler.apply("Error en " + provider.getProviderName() + ": " + e.getMessage());
        }
    }

    /**
     * Verifica si un resultado de negocio es exitoso
     */
    public boolean isSuccessfulResult(Object result) {
        if (result == null) return false;

        if (result instanceof SmsBatchResult) {
            return ((SmsBatchResult) result).isSuccess();
        }
        if (result instanceof OtpSendResult) {
            return ((OtpSendResult) result).isSuccess();
        }
        if (result instanceof SmsCampaignOperationResult) {
            return ((SmsCampaignOperationResult) result).isSuccess();
        }

        return true;
    }
}
