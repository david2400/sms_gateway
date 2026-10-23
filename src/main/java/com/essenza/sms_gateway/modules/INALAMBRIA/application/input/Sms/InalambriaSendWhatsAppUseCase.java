package com.essenza.sms_gateway.modules.INALAMBRIA.application.input.Sms;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendWhatsAppRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;

/** WhatsApp solo por cuentas de Inalambria (ruta INALAMBRIA + tipo WHATSAPP). */
public interface InalambriaSendWhatsAppUseCase {
    DispatchResult send(SendWhatsAppRequest request, String idempotencyKey);
}
