package com.essenza.sms_gateway.modules.SmsCore.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/**
 * Resultado final de un envío, después de reintentos y fallback.
 */
@Value
@Builder(toBuilder = true)
public class DispatchResult {

    public static final String ERROR_VALIDACION = "VALIDACION";
    public static final String ERROR_RUTA_NO_CONFIGURADA = "RUTA_NO_CONFIGURADA";
    public static final String ERROR_TODAS_FALLARON = "TODAS_LAS_CUENTAS_FALLARON";
    public static final String ERROR_CUENTA_NO_VALIDA = "CUENTA_NO_VALIDA";

    UUID envioId;
    DispatchStatus status;
    /** Proveedor con el que se envió (o el último intentado si falló). */
    String providerCode;
    /** Cuenta con la que se envió (o la última intentada si falló). */
    String accountCode;
    Integer httpStatus;
    int attempts;
    /** Código de error de negocio: {@link #ERROR_VALIDACION}, {@link #ERROR_RUTA_NO_CONFIGURADA}, {@link #ERROR_TODAS_FALLARON},
     *  {@link #ERROR_CUENTA_NO_VALIDA}. */
    String errorCode;
    String detail;

    public boolean isSent() {
        return status == DispatchStatus.ENVIADO;
    }
}
