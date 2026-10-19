package com.essenza.sms_gateway.modules.Mock.infrastructure.outbound.providers;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.ports.SmsSenderPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Proveedor simulado para desarrollo y pruebas de fallback. No hace llamadas HTTP.
 * <p>
 * Parámetros opcionales en {@code sms_cuenta.parametros_json}:
 * <ul>
 *   <li>{@code simulateStatus}: HTTP status a devolver (por defecto 200). Ej. 500 para probar reintentos y fallback.</li>
 *   <li>{@code delayMs}: demora simulada.</li>
 * </ul>
 */
@Slf4j
@Component
public class MockSmsSender implements SmsSenderPort {

    public static final String PROVIDER_CODE = "MOCK";

    @Override
    public String providerCode() {
        return PROVIDER_CODE;
    }

    @Override
    public SendOutcome send(SmsDispatch message, ProviderAccount account) {
        long start = System.currentTimeMillis();
        int status = parse(account.parameter("simulateStatus"), 200);
        long delay = parse(account.parameter("delayMs"), 0);
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long elapsed = System.currentTimeMillis() - start;
        log.info("[MOCK-SENDER] Envío {} con cuenta {} → status {}", message.getId(), account.getCode(), status);
        return status >= 200 && status < 300
                ? SendOutcome.ok(status, elapsed)
                : SendOutcome.fail(status, "Fallo simulado por MOCK", elapsed);
    }

    private static int parse(String value, int defaultValue) {
        try {
            return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
