package com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeResponse;

/** Activa o desactiva un tipo de mensaje. */
public interface CambiarEstadoTipoMensajeUseCase {

    /** @throws com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminNotFoundException si no existe */
    TipoMensajeResponse cambiarEstado(Long id, boolean eliminado);
}
