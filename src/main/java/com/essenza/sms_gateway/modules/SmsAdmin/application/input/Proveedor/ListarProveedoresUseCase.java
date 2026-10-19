package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;

import java.util.List;

/** Lista todos los proveedores. */
public interface ListarProveedoresUseCase {
    List<ProveedorResponse> listar();
}
