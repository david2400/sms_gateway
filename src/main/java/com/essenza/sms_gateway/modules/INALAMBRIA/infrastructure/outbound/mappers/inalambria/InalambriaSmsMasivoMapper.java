package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaMasivoRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaRestResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapper ÚNICO del SMS masivo de Inalambria ({@code POST /mtmessage/massive}): request y respuesta.
 * <ul>
 *   <li>Los destinatarios con el MISMO texto van en un solo {@code messages[]}; con plantillas cada texto distinto
 *       genera su elemento. {@code messageId} = envioId del log.</li>
 *   <li>Respuesta: éxito = HTTP 2xx y {@code Status = 0}. {@code Status = 1}, 400, 401 y 403 son permanentes.</li>
 * </ul>
 */
@Mapper(componentModel = "spring", imports = InalambriaMappingSupport.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface InalambriaSmsMasivoMapper {

    String OPT_NOTIFY_URL = "notifyUrl";
    String OPT_NOTIFY_CONTENT_TYPE = "notifyContentType";
    String OPT_X_AUTH = "xAuth";

    // ------------------------------------------------------------------ request

    @Mapping(target = "bulkId", source = "bulkId")
    @Mapping(target = "messages", expression = "java(toMensajes(messages, account))")
    InalambriaMasivoRequest toRequest(List<SmsDispatch> messages, ProviderAccount account, String bulkId);

    @Mapping(target = "to", expression = "java(InalambriaMappingSupport.normalizePhone(message.getTo(), message.getCountryCode()))")
    @Mapping(target = "messageId", source = "id")
    InalambriaMasivoRequest.Destino toDestino(SmsDispatch message);

    List<InalambriaMasivoRequest.Destino> toDestinos(List<SmsDispatch> messages);

    /** Agrupa por texto conservando el orden de llegada. */
    default List<InalambriaMasivoRequest.Mensaje> toMensajes(List<SmsDispatch> messages, ProviderAccount account) {
        boolean flash = InalambriaMappingSupport.isTrue(account.parameter(InalambriaSmsMapper.OPT_FLASH_SMS));
        String notifyUrl = InalambriaMappingSupport.firstNonBlank(account.parameter(OPT_NOTIFY_URL));
        String notifyContentType = notifyUrl == null ? null
                : InalambriaMappingSupport.firstNonBlank(account.parameter(OPT_NOTIFY_CONTENT_TYPE), "application/json");

        Map<String, List<SmsDispatch>> byText = new LinkedHashMap<>();
        messages.forEach(m -> byText.computeIfAbsent(m.getText(), k -> new ArrayList<>()).add(m));

        List<InalambriaMasivoRequest.Mensaje> result = new ArrayList<>(byText.size());
        byText.forEach((text, group) ->
                result.add(new InalambriaMasivoRequest.Mensaje(toDestinos(group), text, flash, notifyUrl, notifyContentType)));
        return result;
    }

    /** Petición HTTP completa; agrega {@code x-auth} si la cuenta lo tiene configurado. */
    default HttpEntity<InalambriaMasivoRequest> toHttpRequest(List<SmsDispatch> messages, ProviderAccount account, String bulkId) {
        HttpHeaders headers = InalambriaMappingSupport.basicAuthJson(account);
        String xAuth = InalambriaMappingSupport.firstNonBlank(account.parameter(OPT_X_AUTH));
        if (xAuth != null) {
            headers.set("x-auth", xAuth);
        }
        return new HttpEntity<>(toRequest(messages, account, bulkId), headers);
    }

    // ----------------------------------------------------------------- response

    default InalambriaRestResponse parseResponse(String body) {
        return InalambriaMappingSupport.parse(body, InalambriaRestResponse.class, InalambriaRestResponse::new,
                InalambriaRestResponse::setRawBody);
    }

    @Mapping(target = "success", expression = "java(InalambriaMappingSupport.is2xx(httpStatus) && !rechazado(response))")
    @Mapping(target = "httpStatus", source = "httpStatus")
    @Mapping(target = "detail", expression = "java(detalle(response, httpStatus))")
    @Mapping(target = "durationMs", source = "durationMs")
    @Mapping(target = "retryable", expression = "java(!InalambriaMappingSupport.isPermanentHttp(httpStatus) && !rechazado(response))")
    SendOutcome toOutcome(InalambriaRestResponse response, int httpStatus, long durationMs);

    /** {@code Status} distinto de 0 = Inalambria rechazó el lote en la validación. */
    default boolean rechazado(InalambriaRestResponse response) {
        return response.getStatus() != null && response.getStatus() != 0;
    }

    default String detalle(InalambriaRestResponse response, int httpStatus) {
        if (rechazado(response)) {
            return "Inalambria rechazó el lote (Status " + response.getStatus() + "): "
                    + InalambriaMappingSupport.truncate(response.getMessageText());
        }
        return InalambriaMappingSupport.is2xx(httpStatus) ? null : InalambriaMappingSupport.truncate(response.getRawBody());
    }
}
