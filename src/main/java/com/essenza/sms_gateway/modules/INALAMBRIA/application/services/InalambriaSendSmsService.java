package com.essenza.sms_gateway.modules.INALAMBRIA.application.services;

import com.essenza.sms_gateway.modules.INALAMBRIA.application.input.Sms.InalambriaSendSmsUseCase;
import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.Sms.InalambriaSendSmsRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaDispatchPayloadMapper;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.providers.inalambria.InalambriaSmsSender;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsByProviderUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Traduce el request propio de Inalambria al mensaje canónico y lo envía por el
 * motor de SmsCore con la ruta (INALAMBRIA, tipo): 3 intentos por cuenta, solo cuentas de Inalambria.
 */
@Service
@RequiredArgsConstructor
public class InalambriaSendSmsService implements InalambriaSendSmsUseCase {

    private final SendSmsByProviderUseCase sendSmsByProvider;

    @Override
    public DispatchResult send(InalambriaSendSmsRequest request, String idempotencyKey) {
        SmsDispatch.SmsDispatchBuilder builder = SmsDispatch.builder()
                .to(request.getTo())
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "57")
                .text(request.getMessage())
                .type(request.getTipoMensaje())
                .accountCode(request.getCuenta())
                .idempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim());

        if (request.getTemplateId() != null) {
            builder.providerOption(InalambriaDispatchPayloadMapper.OPT_TEMPLATE_ID, String.valueOf(request.getTemplateId()));
        }
        if (request.getMessagePattern() != null) {
            builder.providerOption(InalambriaDispatchPayloadMapper.OPT_MESSAGE_PATTERN, request.getMessagePattern());
        }
        if (request.getFlashSms() != null) {
            builder.providerOption(InalambriaDispatchPayloadMapper.OPT_FLASH_SMS, String.valueOf(request.getFlashSms()));
        }
        if (request.getUrl() != null) {
            builder.providerOption(InalambriaDispatchPayloadMapper.OPT_URL, request.getUrl());
        }

        return sendSmsByProvider.send(InalambriaSmsSender.PROVIDER_CODE, builder.build());
    }
}
