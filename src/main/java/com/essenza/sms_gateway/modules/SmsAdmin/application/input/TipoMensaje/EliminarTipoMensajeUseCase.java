package com.essenza.sms_gateway.modules.SmsAdmin.application.input.TipoMensaje;

/** Elimina un tipo de mensaje. */
public interface EliminarTipoMensajeUseCase {

    /** @throws com.essenza.sms_gateway.modules.SmsAdmin.domain.exception.AdminConflictException si hay rutas que lo usan */
    void eliminar(Long id);
}
