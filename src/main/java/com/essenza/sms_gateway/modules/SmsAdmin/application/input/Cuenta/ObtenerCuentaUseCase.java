package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;

/** Obtiene una cuenta por id. */
public interface ObtenerCuentaUseCase {
    CuentaResponse obtener(Long id);
}
