package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;

/** Obtiene un proveedor por id. */
public interface ObtenerProveedorUseCase {
    ProveedorResponse obtener(Long id);
}
