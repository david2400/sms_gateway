package com.essenza.sms_gateway.modules.GestionPlatillas.domain.ports;

import com.essenza.sms_gateway.modules.GestionPlatillas.domain.model.CompiledTemplate;

import java.util.Optional;

/**
 * Plantillas SMS activas ({@code SmsPlantilla} + {@code SmsPlantillaVariable}), ya compiladas.
 * Las implementaciones deben cachear: se consulta en cada envío.
 */
public interface PlantillaQueryPort {

    /**
     * @param code código de la plantilla (sin importar mayúsculas)
     * @return la plantilla activa compilada; vacío si no existe o está inactiva
     * @throws com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException
     *         {@code PLANTILLA_MAL_CONFIGURADA} si existe pero su contenido/variables son inválidos
     */
    Optional<CompiledTemplate> findActive(String code);

    /** Descarta la caché. */
    void refresh();
}
