package com.essenza.sms_gateway.modules.SmsAdmin.infrastructure.outbound.adapters;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.ports.SecretCheckPort;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Revisa en el Environment de Spring si existe la variable de un secret_ref (nunca devuelve el valor). */
@Component
@RequiredArgsConstructor
public class EnvSecretCheckAdapter implements SecretCheckPort {

    private final Environment environment;

    @Override
    public boolean isDefined(String secretRef) {
        if (secretRef == null || secretRef.isBlank()) {
            return false;
        }
        String value = environment.getProperty(secretRef.trim());
        return value != null && !value.isBlank();
    }
}
