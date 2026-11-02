package com.essenza.sms_gateway.modules.SmsCore.domain.model;


import java.util.Locale;

/**
 * Canal administrado por el módulo unificado. Cada canal corresponde a un gateway
 * (sms_gateway / email_gateway) con su propia BD, cifrado de claves y caché de routing.
 */
public enum Canal {
    SMS("sms"),
    EMAIL("email");

    private final String ruta;

    Canal(String ruta) {
        this.ruta = ruta;
    }

    /** Segmento usado en las URLs: {@code /v1/admin/{ruta}/...}. */
    public String getRuta() {
        return ruta;
    }

    /**
     * Resuelve el canal desde el path ({@code sms}, {@code email}; sin importar mayúsculas).
     *
     * @throws IllegalArgumentException si el canal no existe
     */
    public static Canal desdeRuta(String valor) {
        if (valor != null) {
            String v = valor.trim().toLowerCase(Locale.ROOT);
            for (Canal c : values()) {
                if (c.ruta.equals(v) || c.name().equalsIgnoreCase(v)) {
                    return c;
                }
            }
        }
        throw new IllegalArgumentException("Canal desconocido: " + valor + " (use sms o email)");
    }
}
