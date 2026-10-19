package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;

/** Crea un proveedor (código único, en mayúsculas). */
public interface CrearProveedorUseCase {
    ProveedorResponse crear(ProveedorRequest request);
}
