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
    /** El proveedor (o el de la cuenta pedida) no tiene habilitado el tipo de mensaje en {@code SmsProveedorServicio}. */
    public static final String ERROR_SERVICIO_NO_SOPORTADO = "SERVICIO_NO_SOPORTADO";
    /** La ruta pedida en el request ({@code ruta}) no existe. */
    public static final String ERROR_RUTA_NO_ENCONTRADA = "RUTA_NO_ENCONTRADA";
    /** La ruta pedida existe pero está desactivada. */
    public static final String ERROR_RUTA_INACTIVA = "RUTA_INACTIVA";
    /** La ruta pedida es de otro tipo de mensaje que el {@code tipo_mensaje} del request. */
    public static final String ERROR_RUTA_TIPO_NO_COINCIDE = "RUTA_TIPO_NO_COINCIDE";
    /** {@code ruta} no se combina con {@code proveedor} / {@code cuenta}, ni con una ruta de otro proveedor en su endpoint. */
    public static final String ERROR_RUTA_NO_COMBINABLE = "RUTA_NO_COMBINABLE";

    UUID envioId;
    DispatchStatus status;
    /** Proveedor con el que se envió (o el último intentado si falló). */
    String providerCode;
    /** Cuenta con la que se envió (o la última intentada si falló). */
    String accountCode;
    Integer httpStatus;
    int attempts;
    /** Código de error de negocio: {@link #ERROR_VALIDACION}, {@link #ERROR_RUTA_NO_CONFIGURADA}, {@link #ERROR_TODAS_FALLARON},
     *  {@link #ERROR_CUENTA_NO_VALIDA}, {@link #ERROR_SERVICIO_NO_SOPORTADO}, {@link #ERROR_RUTA_NO_ENCONTRADA},
     *  {@link #ERROR_RUTA_INACTIVA}, {@link #ERROR_RUTA_TIPO_NO_COINCIDE}, {@link #ERROR_RUTA_NO_COMBINABLE}. */
    String errorCode;
    String detail;

    public boolean isSent() {
        return status == DispatchStatus.ENVIADO;
    }
}
