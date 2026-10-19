package com.essenza.sms_gateway.modules.SmsAdmin.domain.model;

import lombok.Builder;
import lombok.Value;

/** Paso de una ruta ({@code sms_ruta_paso}). */
@Value
@Builder(toBuilder = true)
public class Paso {
    Long id;
    Long rutaId;
    int orden;
    Long cuentaId;
    String cuentaCodigo;
    String proveedorCodigo;
    int maxIntentos;
    int esperaMs;
    boolean activo;
}
