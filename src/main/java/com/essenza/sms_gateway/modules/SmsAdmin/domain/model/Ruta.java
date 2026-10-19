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
    private String tipoMensaje;
    private Boolean activo;

    public boolean isCentral() {
        return proveedorId == null;
    }
}
