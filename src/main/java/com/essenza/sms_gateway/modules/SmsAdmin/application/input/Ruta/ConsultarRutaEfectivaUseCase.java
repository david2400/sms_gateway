package com.essenza.sms_gateway.modules.SmsAdmin.application.input.Ruta;

import com.essenza.sms_gateway.modules.SmsAdmin.domain.dto.RutaEfectivaResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;

/** Ruta tal como la usará el motor de envío (proveedor null o CENTRAL = central). */
public interface ConsultarRutaEfectivaUseCase {
    RutaEfectivaResponse efectiva(String proveedorCodigo, MessageType tipoMensaje);
}
