package com.essenza.sms_gateway.modules.SmsCore.application.services;

import com.essenza.sms_gateway.modules.SmsCore.application.input.ResolveSmsContentUseCase;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsBatchUseCase;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsByProviderUseCase;
import com.essenza.sms_gateway.modules.SmsCore.application.input.SendSmsUseCase;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsBatchRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendSmsRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsBatchResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SmsEnvioResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.contenido.ResolvedContent;
import com.essenza.sms_gateway.modules.SmsCore.domain.services.DispatchPlanner;
import com.essenza.sms_gateway.modules.SmsCore.domain.services.SmsBatchDispatchService;
import com.essenza.sms_gateway.modules.SmsCore.domain.services.SmsDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Casos de uso de envío. Sin {@code @Transactional}: el log hace sus propias
 * transacciones cortas para no mantener una conexión abierta durante el HTTP.
 * Sin {@code @Retry}: los reintentos los controla el motor ({@link SmsDispatchService}, {@link SmsBatchDispatchService}).
 */
@Service
@RequiredArgsConstructor
public class SmsSendService implements SendSmsUseCase, SendSmsByProviderUseCase, SendSmsBatchUseCase {

    private static final String DEFAULT_COUNTRY = "57";

    private final SmsDispatchService dispatchService;
    private final SmsBatchDispatchService batchDispatchService;
    private final ResolveSmsContentUseCase contentResolver;
    private final DispatchPlanner planner;

    @Override
    public DispatchResult send(SendSmsRequest request, String idempotencyKey) {
        return dispatchService.dispatch(toDispatch(request, idempotencyKey, true), DispatchChannel.CENTRAL, null);
    }

    @Override
    public DispatchResult send(String providerCode, SmsDispatch message) {
        requireProvider(providerCode);
        return dispatchService.dispatch(message, DispatchChannel.PROVEEDOR, providerCode);
    }

    @Override
    public DispatchResult send(String providerCode, SendSmsRequest request, String idempotencyKey) {
        requireProvider(providerCode);
        return dispatchService.dispatch(toDispatch(request, idempotencyKey, false), DispatchChannel.PROVEEDOR, providerCode);
    }

    /**
     * Valida y genera el texto de TODOS los destinatarios antes de enviar; luego un envío del motor por
     * destinatario (lotes nativos si el proveedor del paso los soporta).
     */
    @Override
    public SmsBatchResponse send(SendSmsBatchRequest request, String providerCode) {
        List<SendSmsBatchRequest.Destinatario> destinations = request.getDestinations();
        List<Map<String, String>> perDestination = destinations.stream()
                .map(SendSmsBatchRequest.Destinatario::getVariables).toList();
        ResolvedContent content = contentResolver.resolveBatch(request.getMessage(), request.getPlantilla(),
                request.getVariables(), perDestination, request.getTipoMensaje());
        requireSms(content);

        List<SmsDispatch> messages = new ArrayList<>(destinations.size());
        for (int i = 0; i < destinations.size(); i++) {
            SendSmsBatchRequest.Destinatario d = destinations.get(i);
            messages.add(SmsDispatch.builder()
                    .to(d.getTo())
                    .countryCode(d.getCountryCode() != null ? d.getCountryCode() : DEFAULT_COUNTRY)
                    .text(content.texts().get(i))
                    .type(content.type())
                    .sender(request.getSender())
                    .accountCode(request.getCuenta())
                    .templateCode(content.templateCode())
                    .build());
        }

        String loteId = UUID.randomUUID().toString();
        boolean central = providerCode == null || providerCode.isBlank();
        List<DispatchResult> results = batchDispatchService.dispatch(messages, content.type(),
                central ? DispatchChannel.CENTRAL : DispatchChannel.PROVEEDOR,
                central ? null : providerCode, central ? request.getProveedor() : null, request.getCuenta(), loteId);

        List<SmsEnvioResponse> resultados = results.stream().map(SmsEnvioResponse::from).toList();
        int enviados = (int) results.stream().filter(DispatchResult::isSent).count();
        return SmsBatchResponse.builder()
                .loteId(loteId)
                .plantilla(content.templateCode())
                .total(resultados.size())
                .enviados(enviados)
                .fallidos(resultados.size() - enviados)
                .resultados(resultados)
                .build();
    }

    private SmsDispatch toDispatch(SendSmsRequest request, String idempotencyKey, boolean central) {
        String route = blankToNull(request.getRuta());
        ResolvedContent content = contentResolver.resolve(request.getMessage(), request.getPlantilla(),
                request.getVariables(), typeFor(request, route));
        requireSms(content);
        return SmsDispatch.builder()
                .to(request.getTo())
                .countryCode(request.getCountryCode() != null ? request.getCountryCode() : DEFAULT_COUNTRY)
                .text(content.text())
                .type(content.type())
                .sender(request.getSender())
                .accountCode(request.getCuenta())
                .preferredProvider(central ? request.getProveedor() : null)
                .routeReference(route)
                .templateCode(content.templateCode())
                .idempotencyKey(blankToNull(idempotencyKey))
                .build();
    }

    /**
     * Tipo para resolver el contenido. Con {@code ruta} y sin {@code tipo_mensaje} se usa el tipo de la ruta (también con
     * plantilla). Si la ruta no existe o está inactiva: con texto directo se usa NORMAL solo para armar el mensaje y con
     * plantilla el tipo de la plantilla; el motor responde FALLIDO (RUTA_NO_ENCONTRADA / RUTA_INACTIVA) antes de
     * intentar cualquier envío.
     */
    private MessageType typeFor(SendSmsRequest request, String route) {
        if (request.getTipoMensaje() != null || route == null) {
            return request.getTipoMensaje();
        }
        boolean template = request.getPlantilla() != null && !request.getPlantilla().isBlank();
        return planner.routeType(route).orElse(template ? null : MessageType.NORMAL);
    }

    /** Los endpoints de SMS solo envían tipos de canal SMS (WhatsApp tiene su propio endpoint y payload). */
    private static void requireSms(ResolvedContent content) {
        if (content.type() != null && !content.type().isSms()) {
            throw SmsContenidoException.validacion("El tipo " + content.type() + " es de canal " + content.type().getCanal()
                    + "; use /v1/whatsapp/send");
        }
    }

    private static void requireProvider(String providerCode) {
        if (providerCode == null || providerCode.isBlank()) {
            throw new IllegalArgumentException("providerCode es obligatorio para el envío por proveedor");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
