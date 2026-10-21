package com.essenza.sms_gateway.modules.SmsCore.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;
import java.util.UUID;

/**
 * Mensaje canónico que viaja por el motor de envío. Es independiente del proveedor:
 * cada módulo de proveedor lo traduce a su propio payload.
 */
@Value
@Builder(toBuilder = true)
public class SmsDispatch {

    private static final String PHONE_PATTERN = "\\+?\\d{10,15}";

    /** Identificador del envío (= {@code sms_envio.id}). */
    @Builder.Default
    UUID id = UUID.randomUUID();

    /** Número destino. Acepta 10 dígitos nacionales o con prefijo país, con o sin '+'. */
    String to;

    /** Código de país usado cuando el número llega sin prefijo (por defecto 57). */
    @Builder.Default
    String countryCode = "57";

    String text;

    MessageType type;

    /** Remitente (si el proveedor lo soporta). */
    String sender;

    /** Llave de idempotencia enviada por el cliente (header {@code Idempotency-Key}). */
    String idempotencyKey;

    /**
     * Cuenta pedida por el cliente ({@code sms_cuenta.codigo}). Opcional: si viene, el motor la usa
     * primero y luego continúa con el resto de la ruta como respaldo.
     */
    String accountCode;

    /** Datos propios del proveedor enviados en el request (ej. templateId de Inalambria). */
    @Singular
    Map<String, String> providerOptions;

    /**
     * Valida los datos mínimos antes de gastar intentos contra un proveedor.
     */
    public boolean isValid() {
        return to != null && to.trim().matches(PHONE_PATTERN)
                && text != null && !text.isBlank()
                && type != null;
    }

    public String providerOption(String key) {
        return providerOptions != null ? providerOptions.get(key) : null;
    }
}
