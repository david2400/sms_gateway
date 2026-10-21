package com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeResponse;

/** Crea un tipo de mensaje. */
public interface CrearTipoMensajeUseCase {

    /** @throws com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException si el código ya existe */
    TipoMensajeResponse crear(TipoMensajeRequest request);
}
