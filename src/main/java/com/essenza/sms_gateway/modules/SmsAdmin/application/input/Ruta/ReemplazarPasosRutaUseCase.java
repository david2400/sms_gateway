package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.PasoRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;

import java.util.List;

/** Reemplaza la lista completa de pasos de una ruta. */
public interface ReemplazarPasosRutaUseCase {
    RutaResponse reemplazarPasos(Long id, List<PasoRequest> pasos);
}
