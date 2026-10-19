package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Cuenta;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.CuentaResponse;

import java.util.List;

/** Lista cuentas, con filtro opcional por proveedor. */
public interface ListarCuentasUseCase {
    List<CuentaResponse> listar(Long proveedorId);
}
