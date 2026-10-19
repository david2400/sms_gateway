package com.essenza.sms_gateway.modules.SmsAdmin.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/** Cuenta de un proveedor ({@code sms_cuenta}). Nunca contiene la contraseña, solo su {@code secretRef}. */
@Value
@Builder(toBuilder = true)
public class Cuenta {
    Long id;
    Long proveedorId;
    String proveedorCodigo;
    String codigo;
    String usuario;
    String secretRef;
    String remitente;
    Map<String, Object> parametros;
    boolean activo;
}
