package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaMoMessageRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaMoMessageResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

/**
 * Mapper ÚNICO del SMS bidireccional de Inalambria ({@code POST /momessage}): request y respuesta.
 * <ul>
 *   <li>{@code ShortCode}: el del request ({@code sender}/{@code short_code}), si no {@code SmsCuenta.remitente},
 *       si no {@code parametros_json.shortCode}.</li>
 *   <li>{@code Url}: opción {@code url} del request o de la cuenta (se omite si no hay).</li>
 *   <li>Respuesta: éxito = HTTP 2xx y {@code Status} distinto de false ({@code Message} distinto de ERROR).
 *       Rechazo de validación, 400, 401 y 403 son permanentes.</li>
 * </ul>
 */
@Mapper(componentModel = "spring", imports = InalambriaMappingSupport.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InalambriaSmsBidireccionalMapper {

    String OPT_SHORT_CODE = "shortCode";

    // ------------------------------------------------------------------ request

    @Mapping(target = "messageText", source = "message.text")
    @Mapping(target = "phoneNumber", expression = "java(InalambriaMappingSupport.normalizePhone(message.getTo(), message.getCountryCode()))")
    @Mapping(target = "shortCode", expression = "java(shortCode(message, account))")
    @Mapping(target = "url", expression = "java(InalambriaMappingSupport.option(message, account, InalambriaSmsMapper.OPT_URL, null))")
    InalambriaMoMessageRequest toRequest(SmsDispatch message, ProviderAccount account);

    /** Código corto efectivo o {@code null} si no está configurado. */
    default String shortCode(SmsDispatch message, ProviderAccount account) {
        return InalambriaMappingSupport.firstNonBlank(message.getSender(), account.getSender(), account.parameter(OPT_SHORT_CODE));
    }

    /** JSON + Basic; agrega {@code x-auth} si la cuenta lo tiene ({@code parametros_json.xAuth}). */
    default HttpEntity<InalambriaMoMessageRequest> toHttpRequest(SmsDispatch message, ProviderAccount account) {
        HttpHeaders headers = InalambriaMappingSupport.basicAuthJson(account);
        String xAuth = InalambriaMappingSupport.firstNonBlank(account.parameter(InalambriaSmsMasivoMapper.OPT_X_AUTH));
        if (xAuth != null) {
            headers.set("x-auth", xAuth);
        }
        return new HttpEntity<>(toRequest(message, account), headers);
    }

    // ----------------------------------------------------------------- response

    default InalambriaMoMessageResponse parseResponse(String body) {
        return InalambriaMappingSupport.parse(body, InalambriaMoMessageResponse.class, InalambriaMoMessageResponse::new,
                InalambriaMoMessageResponse::setRawBody);
    }

    @Mapping(target = "success", expression = "java(InalambriaMappingSupport.is2xx(httpStatus) && !rechazado(response))")
    @Mapping(target = "httpStatus", source = "httpStatus")
    @Mapping(target = "detail", expression = "java(InalambriaMappingSupport.is2xx(httpStatus) && !rechazado(response) ? null : detalle(response))")
    @Mapping(target = "durationMs", source = "durationMs")
    @Mapping(target = "retryable", expression = "java(!InalambriaMappingSupport.isPermanentHttp(httpStatus) && !rechazado(response))")
    SendOutcome toOutcome(InalambriaMoMessageResponse response, int httpStatus, long durationMs);

    /** Inalambria respondió que el mensaje no pasó la validación. */
    default boolean rechazado(InalambriaMoMessageResponse r) {
        return Boolean.FALSE.equals(r.getStatus()) || "ERROR".equalsIgnoreCase(r.getMessage());
    }

    default String detalle(InalambriaMoMessageResponse r) {
        if (rechazado(r)) {
            return "Inalambria rechazó el SMS bidireccional (Message " + r.getMessage() + ")";
        }
        return InalambriaMappingSupport.truncate(r.getRawBody());
    }

    /** Número de transacción exacto (28 dígitos) como texto. */
    default String numeroTransaccion(InalambriaMoMessageResponse r) {
        return r.getId() == null ? null : r.getId().toPlainString();
    }
}
