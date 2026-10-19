package com.essenza.sms_gateway.modules.INALAMBRIA.application.input.Sms;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.Sms.InalambriaSendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;

/**
 * Envío de SMS solo por cuentas de Inalambria (ruta INALAMBRIA + tipo en BD).
 */
public interface InalambriaSendSmsUseCase {

    DispatchResult send(InalambriaSendSmsRequest request, String idempotencyKey);
}
