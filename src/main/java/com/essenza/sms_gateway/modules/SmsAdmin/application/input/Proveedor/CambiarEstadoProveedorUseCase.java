package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Proveedor;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.ProveedorResponse;

/** Activa o desactiva un proveedor (no hay borrado: el log de envíos lo referencia). */
public interface CambiarEstadoProveedorUseCase {
    ProveedorResponse cambiarEstado(Long id, boolean activo);
}
