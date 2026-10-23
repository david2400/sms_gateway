package com.essenza.sms_gateway.modules.GestionPlatillas.domain.services;

import java.util.Locale;

/** Normalización de códigos (se guardan en mayúsculas, sin espacios). */
public final class CodigosPlantilla {

    private CodigosPlantilla() {
    }

    public static String normalizar(String codigo) {
        return codigo == null ? null : codigo.trim().toUpperCase(Locale.ROOT);
    }

    public static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
