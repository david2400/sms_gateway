package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;

/** Actualiza los datos de un proveedor. */
public interface ActualizarProveedorUseCase {
    ProveedorResponse actualizar(Long id, ProveedorRequest request);
}
