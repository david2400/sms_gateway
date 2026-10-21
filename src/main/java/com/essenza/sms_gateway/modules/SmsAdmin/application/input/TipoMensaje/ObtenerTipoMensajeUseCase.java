package com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeResponse;

/** Obtiene un tipo de mensaje. */
public interface ObtenerTipoMensajeUseCase {

    /** @throws com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminNotFoundException si no existe */
    TipoMensajeResponse obtener(Long id);

    /** @throws com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminNotFoundException si el código no existe */
    TipoMensajeResponse obtenerPorCodigo(String codigo);
}
