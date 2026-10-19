package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;

/** Obtiene una ruta con sus pasos. */
public interface ObtenerRutaUseCase {
    RutaResponse obtener(Long id);
}
