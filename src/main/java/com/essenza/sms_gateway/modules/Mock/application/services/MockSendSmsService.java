package com.essenza.sms_gateway.modules.Mock.application.services;

import com.essenza.sms_gateway.modules.Mock.application.input.MockSendSmsUseCase;
import com.essenza.sms_gateway.modules.Mock.infrastructure.outbound.providers.MockSmsSender;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsByProviderUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MockSendSmsService implements MockSendSmsUseCase {

    private final SendSmsByProviderUseCase sendSmsByProvider;

    @Override
    public DispatchResult send(SendSmsRequest request, String idempotencyKey) {
        SmsDispatch message = SmsDispatch.builder()
                .to(request.getTo())
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "57")
                .text(request.getMessage())
                .type(request.getTipoMensaje())
                .sender(request.getSender())
                .accountCode(request.getCuenta())
                .idempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim())
                .build();
        return sendSmsByProvider.send(MockSmsSender.PROVIDER_CODE, message);
    }
}
