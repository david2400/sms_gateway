package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;

/** Crea una ruta (proveedor null = central), opcionalmente con pasos. */
public interface CrearRutaUseCase {
    RutaResponse crear(RutaRequest request);
}
