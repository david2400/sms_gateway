package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaWhatsAppRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaWhatsAppResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.http.HttpEntity;

/**
 * Mapper ÚNICO de WhatsApp de Inalambria ({@code POST /MultiChannelInput/SendMessageMetaWhatsApp}): request y respuesta.
 * <ul>
 *   <li>{@code source}: el del request, si no {@code SmsCuenta.remitente}, si no {@code parametros_json.source}.</li>
 *   <li>Respuesta: éxito = HTTP 2xx, {@code httpStatusCode} 200 (o ausente) y {@code transactionNumber} distinto de 0.
 *       400/401/403 (del HTTP o del body) son permanentes.</li>
 * </ul>
 */
@Mapper(componentModel = "spring", imports = {InalambriaMappingSupport.class, java.util.List.class, java.util.Map.class},
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InalambriaWhatsAppMapper {

    String OPT_SOURCE = "source";

    // ------------------------------------------------------------------ request

    @Mapping(target = "source", expression = "java(source(message, account))")
    @Mapping(target = "destination", expression = "java(InalambriaMappingSupport.normalizePhone(message.getTo(), message.getCountryCode()))")
    @Mapping(target = "templateId", source = "message.externalTemplateId")
    @Mapping(target = "parameters", source = "message.templateParameters", defaultExpression = "java(List.of())")
    @Mapping(target = "optionalAditionalData", source = "message.additionalData", defaultExpression = "java(Map.of())")
    InalambriaWhatsAppRequest toRequest(SmsDispatch message, ProviderAccount account);

    /** Origen efectivo con prefijo país, o {@code null} si no está configurado. */
    default String source(SmsDispatch message, ProviderAccount account) {
        String source = InalambriaMappingSupport.firstNonBlank(message.getSender(), account.getSender(), account.parameter(OPT_SOURCE));
        return source == null ? null : InalambriaMappingSupport.normalizePhone(source, message.getCountryCode());
    }

    default HttpEntity<InalambriaWhatsAppRequest> toHttpRequest(SmsDispatch message, ProviderAccount account) {
        return new HttpEntity<>(toRequest(message, account), InalambriaMappingSupport.basicAuthJson(account));
    }

    // ----------------------------------------------------------------- response

    default InalambriaWhatsAppResponse parseResponse(String body) {
        return InalambriaMappingSupport.parse(body, InalambriaWhatsAppResponse.class, InalambriaWhatsAppResponse::new,
                InalambriaWhatsAppResponse::setRawBody);
    }

    @Mapping(target = "success", expression = "java(InalambriaMappingSupport.is2xx(httpStatus) && aceptado(response))")
    @Mapping(target = "httpStatus", expression = "java(estadoEfectivo(response, httpStatus))")
    @Mapping(target = "detail", expression = "java(InalambriaMappingSupport.is2xx(httpStatus) && aceptado(response) ? null : detalle(response))")
    @Mapping(target = "durationMs", source = "durationMs")
    @Mapping(target = "retryable", expression = "java(!InalambriaMappingSupport.isPermanentHttp(estadoEfectivo(response, httpStatus)))")
    SendOutcome toOutcome(InalambriaWhatsAppResponse response, int httpStatus, long durationMs);

    default boolean aceptado(InalambriaWhatsAppResponse r) {
        boolean statusOk = r.getHttpStatusCode() == null || r.getHttpStatusCode() == 200;
        return statusOk && r.getTransactionNumber() != null && r.getTransactionNumber().signum() != 0;
    }

    /** El código del body manda cuando Inalambria responde 200 con un error dentro. */
    default int estadoEfectivo(InalambriaWhatsAppResponse r, int httpStatus) {
        return !aceptado(r) && r.getHttpStatusCode() != null ? r.getHttpStatusCode() : httpStatus;
    }

    /** Número de transacción exacto (28 dígitos) como texto. */
    default String numeroTransaccion(InalambriaWhatsAppResponse r) {
        return r.getTransactionNumber() == null ? null : r.getTransactionNumber().toPlainString();
    }

    default String detalle(InalambriaWhatsAppResponse r) {
        StringBuilder sb = new StringBuilder();
        if (r.getMessageText() != null) {
            sb.append(r.getMessageText());
        }
        if (r.getErrors() != null) {
            sb.append(sb.isEmpty() ? "" : ": ").append(r.getErrors());
        }
        if (r.getErrorDetails() != null) {
            sb.append(sb.isEmpty() ? "" : " - ").append(r.getErrorDetails());
        }
        return InalambriaMappingSupport.truncate(sb.isEmpty() ? r.getRawBody() : sb.toString());
    }
}
