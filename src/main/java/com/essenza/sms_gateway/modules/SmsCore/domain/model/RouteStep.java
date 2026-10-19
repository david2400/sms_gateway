package com.essenza.sms_gateway.modules.SmsCore.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Paso de una ruta ({@code sms_ruta_paso}): una cuenta con su política de intentos.
 */
@Value
@Builder
public class RouteStep {

    /** 1 = principal, 2.. = fallback. */
    int order;
    ProviderAccount account;
    /** Intentos máximos sobre esta cuenta antes de pasar al siguiente paso (por defecto 3). */
    int maxAttempts;
    /** Espera entre intentos fallidos, en milisegundos. */
    long waitMs;
}
