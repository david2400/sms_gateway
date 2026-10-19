package com.essenza.sms_gateway.modules.SmsAdmin.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/** Cuenta de un proveedor ({@code sms_cuenta}). Nunca contiene la contraseña, solo su {@code secretRef}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cuenta {
    private Long id;
    private Long proveedorId;
    private String proveedorCodigo;
    private String codigo;
    private String usuario;
    private String secretRef;
    private String remitente;
    private Map<String, Object> parametros = new LinkedHashMap<>();
    private Boolean activo;
}
