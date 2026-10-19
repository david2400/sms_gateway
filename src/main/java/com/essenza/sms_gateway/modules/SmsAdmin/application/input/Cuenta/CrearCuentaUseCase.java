package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;

/** Crea una cuenta. La contraseña nunca entra: solo el nombre de su variable de entorno. */
public interface CrearCuentaUseCase {
    CuentaResponse crear(CuentaRequest request);
}
