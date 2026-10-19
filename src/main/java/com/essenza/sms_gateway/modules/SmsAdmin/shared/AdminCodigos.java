package com.essenza.sms_gateway.modules.SmsAdmin.shared;

/** Normalización de códigos de proveedor y cuenta (se guardan en mayúsculas, sin espacios). */
public final class AdminCodigos {

    public static final String CENTRAL = "CENTRAL";

    private AdminCodigos() {
    }

    public static String normalizar(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase();
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
