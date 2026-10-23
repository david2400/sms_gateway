package com.essenza.sms_gateway.modules.INALAMBRIA.application.services;

import com.essenza.sms_gateway.modules.INALAMBRIA.application.input.Sms.InalambriaSendSmsUseCase;
import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.Sms.InalambriaSendSmsRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria.InalambriaSmsMapper;
import com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.providers.inalambria.InalambriaSmsSender;
import com.essenza.sms_gateway.modules.SmsCore.application.input.ResolveSmsContentUseCase;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsByProviderUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.ResolvedContent;
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
    private final ResolveSmsContentUseCase contentResolver;

    @Override
    public DispatchResult send(InalambriaSendSmsRequest request, String idempotencyKey) {
        ResolvedContent content = contentResolver.resolve(request.getMessage(), request.getPlantilla(),
                request.getVariables(), request.getTipoMensaje());
        SmsDispatch.SmsDispatchBuilder builder = SmsDispatch.builder()
                .to(request.getTo())
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "57")
                .text(content.text())
                .type(content.type())
                .templateCode(content.templateCode())
                .sender(request.getShortCode() == null || request.getShortCode().isBlank() ? null : request.getShortCode().trim())
                .accountCode(request.getCuenta())
                .idempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim());

        if (request.getTemplateId() != null) {
            builder.providerOption(InalambriaSmsMapper.OPT_TEMPLATE_ID, String.valueOf(request.getTemplateId()));
        }
        if (request.getMessagePattern() != null) {
            builder.providerOption(InalambriaSmsMapper.OPT_MESSAGE_PATTERN, request.getMessagePattern());
        }
        if (request.getFlashSms() != null) {
            builder.providerOption(InalambriaSmsMapper.OPT_FLASH_SMS, String.valueOf(request.getFlashSms()));
        }
        if (request.getUrl() != null) {
            builder.providerOption(InalambriaSmsMapper.OPT_URL, request.getUrl());
        }

        return sendSmsByProvider.send(InalambriaSmsSender.PROVIDER_CODE, builder.build());
    }
}
