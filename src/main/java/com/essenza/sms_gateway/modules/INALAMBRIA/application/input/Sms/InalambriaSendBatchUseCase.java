package com.essenza.sms_gateway.modules.INALAMBRIA.application.input.Sms;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsBatchRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsBatchResponse;

/**
 * Envío masivo solo por cuentas de Inalambria (ruta INALAMBRIA + tipo), con la API masiva nativa.
 */
public interface InalambriaSendBatchUseCase {

    SmsBatchResponse send(SendSmsBatchRequest request);
}
