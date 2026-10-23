package com.essenza.sms_gateway.modules.GestionPlatillas.domain.exception;

import java.util.List;

/** Plantilla inválida (400). {@code detalles} lista todos los errores encontrados. */
public class PlantillaValidacionException extends RuntimeException {

    private final List<String> detalles;

    public PlantillaValidacionException(String message) {
        this(message, List.of());
    }

    public PlantillaValidacionException(String message, List<String> detalles) {
        super(message);
        this.detalles = detalles == null ? List.of() : List.copyOf(detalles);
    }

    public List<String> getDetalles() {
        return detalles;
    }
}
