package com.essenza.sms_gateway.modules.SmsAdmin.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ruta ({@code sms_ruta}). {@code proveedorId == null} = ruta del endpoint central. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ruta {
    private Long id;
    private Long proveedorId;
    private String proveedorCodigo;
    /** FK a {@code sms_tipo_mensaje}. */
    private Long tipoMensajeId;
    /** Código del tipo (derivado; ej. OTP). */
    private String tipoMensaje;
    private Boolean eliminado;

    public boolean isCentral() {
        return proveedorId == null;
    }
}
