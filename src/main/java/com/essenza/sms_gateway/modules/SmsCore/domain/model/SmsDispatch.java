package com.essenza.sms_gateway.modules.SmsCore.domain.model;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
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

    /**
     * Proveedor preferido en el endpoint CENTRAL ({@code sms_proveedor.codigo}). Opcional: sus cuentas van
     * primero y el resto de la ruta central del mismo tipo queda como respaldo. Debe tener el servicio habilitado.
     */
    String preferredProvider;

    /**
     * Ruta pedida por el cliente (código o id de {@code CanalRuta}). Opcional: si viene, el motor ejecuta los pasos de
     * esa ruta en lugar de resolverla por tipo de mensaje / proveedor.
     */
    String routeReference;

    /** Código de la plantilla con la que se generó {@link #text} (solo para el log). */
    String templateCode;

    /**
     * Canal WhatsApp: identificador de la plantilla aprobada en el proveedor/Meta. En WhatsApp no se envía
     * texto libre: el proveedor arma el mensaje con esta plantilla y {@link #templateParameters}.
     */
    String externalTemplateId;

    /** Canal WhatsApp: valores de la plantilla del proveedor, en el orden que ella exige. */
    @Singular
    List<String> templateParameters;

    /** Datos adicionales que el proveedor devuelve o usa (ej. {@code optionalAditionalData} de Inalambria). */
    @Singular("additionalDataEntry")
    Map<String, String> additionalData;

    /** Datos propios del proveedor enviados en el request (ej. templateId de Inalambria). */
    @Singular
    Map<String, String> providerOptions;

    /**
     * Valida los datos mínimos antes de gastar intentos contra un proveedor.
     */
    public boolean isValid() {
        if (to == null || !to.trim().matches(PHONE_PATTERN) || type == null) {
            return false;
        }
        if (type.isWhatsapp()) {
            return externalTemplateId != null && !externalTemplateId.isBlank()
                    && (templateParameters == null || templateParameters.stream().noneMatch(p -> p == null || p.isBlank()));
        }
        return text != null && !text.isBlank();
    }

    public String providerOption(String key) {
        return providerOptions != null ? providerOptions.get(key) : null;
    }
}
