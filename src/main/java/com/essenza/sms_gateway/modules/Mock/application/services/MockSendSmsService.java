package com.essenza.sms_gateway.modules.Mock.application.services;

import com.essenza.sms_gateway.modules.Mock.application.input.MockSendSmsUseCase;
import com.essenza.sms_gateway.modules.Mock.infrastructure.outbound.providers.MockSmsSender;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsByProviderUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MockSendSmsService implements MockSendSmsUseCase {

    private final SendSmsByProviderUseCase sendSmsByProvider;

    @Override
    public DispatchResult send(SendSmsRequest request, String idempotencyKey) {
        // Texto directo o plantilla + variables: lo resuelve SmsCore (validación estricta de variables)
        return sendSmsByProvider.send(MockSmsSender.PROVIDER_CODE, request, idempotencyKey);
    }

}
