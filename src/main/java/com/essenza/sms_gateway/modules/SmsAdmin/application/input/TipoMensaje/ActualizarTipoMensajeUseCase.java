package com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.TipoMensajeResponse;

/** Actualiza un tipo de mensaje. El código no se puede cambiar (lo referencian las rutas). */
public interface ActualizarTipoMensajeUseCase {

    /** @throws com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminNotFoundException si no existe */
    TipoMensajeResponse actualizar(Long id, TipoMensajeRequest request);
}
