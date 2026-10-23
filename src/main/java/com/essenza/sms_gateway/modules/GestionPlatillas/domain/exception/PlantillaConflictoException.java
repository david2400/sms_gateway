package com.essenza.sms_gateway.modules.GestionPlatillas.domain.exception;

/** Conflicto con otra plantilla, ej. código repetido (409). */
public class PlantillaConflictoException extends RuntimeException {
    public PlantillaConflictoException(String message) {
        super(message);
    }
}
