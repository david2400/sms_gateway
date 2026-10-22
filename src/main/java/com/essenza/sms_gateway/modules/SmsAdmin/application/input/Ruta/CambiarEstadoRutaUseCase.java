package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;

/** Activa o desactiva una ruta. */
public interface CambiarEstadoRutaUseCase {
    RutaResponse cambiarEstado(Long id, boolean eliminado);
}
