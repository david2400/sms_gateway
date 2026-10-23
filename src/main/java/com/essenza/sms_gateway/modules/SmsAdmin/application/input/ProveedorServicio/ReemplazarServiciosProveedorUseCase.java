package com.essenza.sms_gateway.modules.SmsAdmin.application.input.ProveedorServicio;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ServiciosProveedorRequest;
import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ServiciosProveedorResponse;

public interface ReemplazarServiciosProveedorUseCase {
    ServiciosProveedorResponse reemplazar(Long proveedorId, ServiciosProveedorRequest request);
}
