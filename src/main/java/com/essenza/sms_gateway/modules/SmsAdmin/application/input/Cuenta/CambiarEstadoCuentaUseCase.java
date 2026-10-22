package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;

/** Activa o desactiva una cuenta (no hay borrado: el log de envíos la referencia). */
public interface CambiarEstadoCuentaUseCase {
    CuentaResponse cambiarEstado(Long id, boolean eliminado);
}
