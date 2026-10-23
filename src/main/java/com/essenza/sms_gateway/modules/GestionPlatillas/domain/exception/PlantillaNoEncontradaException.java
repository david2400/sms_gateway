package com.essenza.sms_gateway.modules.GestionPlatillas.domain.exception;

/** No existe la plantilla (404). */
public class PlantillaNoEncontradaException extends RuntimeException {
    public PlantillaNoEncontradaException(String message) {
        super(message);
    }
}
