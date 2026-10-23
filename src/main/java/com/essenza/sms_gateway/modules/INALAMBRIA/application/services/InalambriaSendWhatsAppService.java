package com.essenza.sms_gateway.modules.INALAMBRIA.application.services;

import com.essenza.sms_gateway.modules.INALAMBRIA.application.input.Sms.InalambriaSendWhatsAppUseCase;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.providers.inalambria.InalambriaSmsSender;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendWhatsAppUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendWhatsAppRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InalambriaSendWhatsAppService implements InalambriaSendWhatsAppUseCase {

    private final SendWhatsAppUseCase sendWhatsApp;

    @Override
    public DispatchResult send(SendWhatsAppRequest request, String idempotencyKey) {
        return sendWhatsApp.send(request, idempotencyKey, InalambriaSmsSender.PROVIDER_CODE);
    }
}
