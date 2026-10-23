package com.essenza.sms_gateway.modules.SmsAdmin.application.input.ProveedorServicio;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ServiciosProveedorResponse;

public interface ListarServiciosProveedorUseCase {
    ServiciosProveedorResponse listar(Long proveedorId);
}
