package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaMtMessageRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaRestResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.http.HttpEntity;

/**
 * Mapper ÚNICO del SMS individual de Inalambria ({@code POST /MTMessage}, RESTAWS ver 3.1): request y respuesta.
 * <p>
 * Datos propios de Inalambria, en orden de prioridad: {@code providerOptions} del request (endpoint de Inalambria),
 * {@code SmsCuenta.parametros_json}, valor por defecto. Claves: {@code type}, {@code messagePattern},
 * {@code templateId}, {@code flashSms}, {@code url}.
 * <p>
 * Respuesta: éxito = HTTP 2xx (no se interpreta el body, decisión vigente del proyecto); el body va al detalle si falla.
 */
@Mapper(componentModel = "spring", imports = InalambriaMappingSupport.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InalambriaSmsMapper {

    String OPT_TYPE = "type";
    String OPT_MESSAGE_PATTERN = "messagePattern";
    String OPT_TEMPLATE_ID = "templateId";
    String OPT_FLASH_SMS = "flashSms";
    String OPT_URL = "url";

    // ------------------------------------------------------------------ request

    @Mapping(target = "type", expression = "java(InalambriaMappingSupport.intOption(message, account, OPT_TYPE, 1))")
    @Mapping(target = "messageText", source = "message.text")
    @Mapping(target = "devices", expression = "java(InalambriaMappingSupport.normalizePhone(message.getTo(), message.getCountryCode()))")
    @Mapping(target = "dateMessage", expression = "java(InalambriaMappingSupport.now())")
    @Mapping(target = "flashSms", expression = "java(InalambriaMappingSupport.flagOption(message, account, OPT_FLASH_SMS))")
    @Mapping(target = "hasMore", constant = "0")
    @Mapping(target = "messagePattern", expression = "java(InalambriaMappingSupport.option(message, account, OPT_MESSAGE_PATTERN, \"0\"))")
    @Mapping(target = "templateId", expression = "java(InalambriaMappingSupport.intOption(message, account, OPT_TEMPLATE_ID, 0))")
    @Mapping(target = "transactionNumber", expression = "java(InalambriaMappingSupport.newTransactionNumber())")
    @Mapping(target = "url", expression = "java(InalambriaMappingSupport.option(message, account, OPT_URL, \"\"))")
    InalambriaMtMessageRequest toRequest(SmsDispatch message, ProviderAccount account);

    /** Petición HTTP completa: body + JSON + Basic con la cuenta. */
    default HttpEntity<InalambriaMtMessageRequest> toHttpRequest(SmsDispatch message, ProviderAccount account) {
        return new HttpEntity<>(toRequest(message, account), InalambriaMappingSupport.basicAuthJson(account));
    }

    // ----------------------------------------------------------------- response

    default InalambriaRestResponse parseResponse(String body) {
        return InalambriaMappingSupport.parse(body, InalambriaRestResponse.class, InalambriaRestResponse::new,
                InalambriaRestResponse::setRawBody);
    }

    @Mapping(target = "success", expression = "java(InalambriaMappingSupport.is2xx(httpStatus))")
    @Mapping(target = "httpStatus", source = "httpStatus")
    @Mapping(target = "detail", expression = "java(InalambriaMappingSupport.is2xx(httpStatus) ? null : InalambriaMappingSupport.truncate(response.getRawBody()))")
    @Mapping(target = "durationMs", source = "durationMs")
    @Mapping(target = "retryable", constant = "true")
    SendOutcome toOutcome(InalambriaRestResponse response, int httpStatus, long durationMs);
}
