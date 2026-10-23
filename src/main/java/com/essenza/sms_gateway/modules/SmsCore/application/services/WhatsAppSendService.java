package com.essenza.sms_gateway.modules.SmsCore.application.services;

import com.essenza.sms_gateway.modules.SmsCore.application.input.SendWhatsAppUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendWhatsAppRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.services.SmsDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Construye el mensaje canónico de WhatsApp y lo envía con el motor. El texto del log es una descripción
 * legible ({@code [WhatsApp <plantilla>] p1 | p2}) porque el contenido real lo arma el proveedor.
 */
@Service
@RequiredArgsConstructor
public class WhatsAppSendService implements SendWhatsAppUseCase {

    private static final int MAX_LOG_TEXT = 1600;

    private final SmsDispatchService dispatchService;

    @Override
    public DispatchResult send(SendWhatsAppRequest request, String idempotencyKey, String providerCode) {
        MessageType type = request.getTipoMensaje() != null ? request.getTipoMensaje() : MessageType.WHATSAPP;
        if (!type.isWhatsapp()) {
            throw SmsContenidoException.validacion("El tipo " + type + " no es de canal WhatsApp; use /v1/sms/send");
        }
        List<String> parameters = request.getParameters() != null ? request.getParameters() : List.of();
        boolean central = providerCode == null || providerCode.isBlank();

        SmsDispatch.SmsDispatchBuilder builder = SmsDispatch.builder()
                .to(request.getTo())
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "57")
                .type(type)
                .externalTemplateId(request.getTemplateId().trim())
                .templateParameters(parameters)
                .text(describe(request.getTemplateId().trim(), parameters))
                .sender(blankToNull(request.getSource()))
                .accountCode(request.getCuenta())
                .preferredProvider(central ? request.getProveedor() : null)
                .idempotencyKey(blankToNull(idempotencyKey));
        if (request.getDatosAdicionales() != null) {
            builder.additionalData(request.getDatosAdicionales());
        }
        SmsDispatch message = builder.build();
        return central
                ? dispatchService.dispatch(message, DispatchChannel.CENTRAL, null)
                : dispatchService.dispatch(message, DispatchChannel.PROVEEDOR, providerCode);
    }

    static String describe(String templateId, List<String> parameters) {
        String text = "[WhatsApp " + templateId + "] " + String.join(" | ", parameters);
        return text.length() <= MAX_LOG_TEXT ? text.trim() : text.substring(0, MAX_LOG_TEXT);
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
