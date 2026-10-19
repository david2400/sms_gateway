package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;

import java.util.List;

/** Lista rutas con filtros opcionales. */
public interface ListarRutasUseCase {
    List<RutaResponse> listar(Long proveedorId, boolean soloCentral, MessageType tipoMensaje);
}
