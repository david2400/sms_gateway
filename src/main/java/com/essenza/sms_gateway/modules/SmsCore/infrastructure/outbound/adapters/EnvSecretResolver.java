package com.essenza.sms_gateway.modules.SmsCore.infrastructure.outbound.adapters;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Resuelve el {@code secret_ref} de una cuenta. El valor se busca en el {@link Environment}
 * de Spring, así que puede venir de una variable de entorno (ej. {@code INALAMBRIA_OTP_PASSWORD})
 * o de una propiedad externa. Nunca se guarda la contraseña en BD.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvSecretResolver {

    private final Environment environment;

    public String resolve(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            return null;
        }
        String value = environment.getProperty(secretRef.trim());
        if (value == null || value.isBlank()) {
            log.warn("[SMS-CORE] El secreto '{}' no está definido en el entorno", secretRef);
            return null;
        }
        return value;
    }
}
