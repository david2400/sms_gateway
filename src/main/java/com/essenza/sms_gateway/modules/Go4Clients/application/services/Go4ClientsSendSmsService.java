package com.essenza.sms_gateway.modules.Go4Clients.application.services;

import com.essenza.sms_gateway.modules.Go4Clients.application.input.Go4ClientsSendSmsUseCase;
import com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.providers.Go4ClientsSmsSender;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsByProviderUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Go4ClientsSendSmsService implements Go4ClientsSendSmsUseCase {

    private final SendSmsByProviderUseCase sendSmsByProvider;

    @Override
    public DispatchResult send(SendSmsRequest request, String idempotencyKey) {
        // Texto directo o plantilla + variables: lo resuelve SmsCore (validación estricta de variables)
        return sendSmsByProvider.send(Go4ClientsSmsSender.PROVIDER_CODE, request, idempotencyKey);
    }

}
