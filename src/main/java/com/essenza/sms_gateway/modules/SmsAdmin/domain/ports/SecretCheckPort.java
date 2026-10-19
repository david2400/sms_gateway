package com.essenza.sms_gateway.modules.SmsAdmin.domain.ports;

/** Verifica si la variable de entorno de un secret_ref existe, sin exponer su valor. */
public interface SecretCheckPort {
    boolean isDefined(String secretRef);
}
