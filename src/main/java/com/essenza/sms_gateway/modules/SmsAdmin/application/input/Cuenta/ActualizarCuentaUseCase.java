package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;

/** Actualiza una cuenta. */
public interface ActualizarCuentaUseCase {
    CuentaResponse actualizar(Long id, CuentaRequest request);
}
