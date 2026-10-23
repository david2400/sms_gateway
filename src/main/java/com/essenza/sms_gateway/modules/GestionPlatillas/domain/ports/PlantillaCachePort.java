package com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports;

/** Invalida la caché de plantillas compiladas cuando cambia la configuración. */
public interface PlantillaCachePort {
    /** Programa la invalidación para después del commit de la transacción en curso. */
    void invalidarAlConfirmar();
}
