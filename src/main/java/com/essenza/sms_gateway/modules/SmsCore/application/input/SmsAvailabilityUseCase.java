package com.essenza.sms_gateway.modules.SmsCore.application.input;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;

import java.util.Optional;

/**
 * Disponibilidad del servicio según las rutas configuradas en BD.
 */
public interface SmsAvailabilityUseCase {

    /** true si existe una ruta central activa con al menos una cuenta para el tipo. */
    boolean isAvailable(MessageType type);

    /** Proveedor principal (paso 1) de la ruta central del tipo. */
    Optional<String> primaryProvider(MessageType type);
}
