package com.essenza.sms_gateway.modules.SmsAdmin.domain.model;

import lombok.Builder;
import lombok.Value;

/** Ruta ({@code sms_ruta}). {@code proveedorId == null} = ruta del endpoint central. */
@Value
@Builder(toBuilder = true)
public class Ruta {
    Long id;
    Long proveedorId;
    String proveedorCodigo;
    String tipoMensaje;
    boolean activo;

    public boolean isCentral() {
        return proveedorId == null;
    }
}
