package com.essenza.sms_gateway.modules.SmsCore.application.services;

import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsBatchUseCase;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsByProviderUseCase;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsBatchRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsBatchResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.services.SmsDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Casos de uso de envío. Sin {@code @Transactional}: el log hace sus propias
 * transacciones cortas para no mantener una conexión abierta durante el HTTP.
 * Sin {@code @Retry}: los reintentos los controla {@link SmsDispatchService}.
 */
@Service
@RequiredArgsConstructor
public class SmsSendService implements SendSmsUseCase, SendSmsByProviderUseCase, SendSmsBatchUseCase {

    private final SmsDispatchService dispatchService;

    @Override
    public DispatchResult send(SendSmsRequest request, String idempotencyKey) {
        SmsDispatch message = SmsDispatch.builder()
                .to(request.getTo())
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : "57")
                .text(request.getMessage())
                .type(request.getTipoMensaje())
                .sender(request.getSender())
                .accountCode(request.getCuenta())
                .idempotencyKey(blankToNull(idempotencyKey))
                .build();
        return dispatchService.dispatch(message, DispatchChannel.CENTRAL, null);
    }

    @Override
    public DispatchResult send(String providerCode, SmsDispatch message) {
        if (providerCode == null || providerCode.isBlank()) {
            throw new IllegalArgumentException("providerCode es obligatorio para el envío por proveedor");
        }
        return dispatchService.dispatch(message, DispatchChannel.PROVEEDOR, providerCode);
    }

    /**
     * Un envío del motor por destinatario, en secuencia. Cada uno queda en el log con sus intentos.
     */
    @Override
    public SmsBatchResponse send(SendSmsBatchRequest request) {
        List<SmsEnvioResponse> resultados = new ArrayList<>();
        for (SendSmsBatchRequest.Destinatario d : request.getDestinations()) {
            SmsDispatch message = SmsDispatch.builder()
                    .to(d.getTo())
                    .countryCode(d.getCountryCode() != null ? d.getCountryCode() : "57")
                    .text(request.getMessage())
                    .type(request.getTipoMensaje())
                    .sender(request.getSender())
                    .accountCode(request.getCuenta())
                    .build();
            resultados.add(SmsEnvioResponse.from(dispatchService.dispatch(message, DispatchChannel.CENTRAL, null)));
        }
        long enviados = resultados.stream().filter(r -> r.getEstado() == DispatchStatus.ENVIADO).count();
        return SmsBatchResponse.builder()
                .total(resultados.size())
                .enviados((int) enviados)
                .fallidos(resultados.size() - (int) enviados)
                .resultados(resultados)
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
