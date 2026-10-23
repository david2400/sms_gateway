package com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.Go4ClientsDestinatarioDto;
import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.api.Go4ClientsMessagesRequest;
import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.api.Go4ClientsMessagesResponse;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.http.HttpEntity;

import java.util.List;
import java.util.Map;

/**
 * Mapper ÚNICO del envío de SMS de una campaña de Go4Clients ({@code POST /campaigns/sms/v1.0/{campaignId}}):
 * request y respuesta. Lo usan el envío individual del motor (una campaña por envío) y las campañas directas.
 * Respuesta {@code { generatedIds: [...] }}; éxito = HTTP 2xx.
 */
@Mapper(componentModel = "spring", imports = Go4ClientsMappingSupport.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface Go4ClientsMessageMapper {

    // ------------------------------------------------------------------ request

    @Mapping(target = "priority", expression = "java(Go4ClientsMappingSupport.orDefault(priority, Go4ClientsMappingSupport.DEFAULT_PRIORITY))")
    @Mapping(target = "message", source = "message")
    @Mapping(target = "destinationsList", source = "destinations")
    @Mapping(target = "landingCustomFields", expression = "java(landingCustomFields == null || landingCustomFields.isEmpty() ? null : landingCustomFields)")
    Go4ClientsMessagesRequest toRequest(String message, List<Go4ClientsDestinatarioDto> destinations, String priority,
                                        Map<String, Map<String, String>> landingCustomFields);

    @Mapping(target = "phoneNumber", expression = "java(Go4ClientsMappingSupport.normalizePhone(destinatario.getPhoneNumber()))")
    @Mapping(target = "countryCode", expression = "java(Go4ClientsMappingSupport.orDefault(destinatario.getCountryCode(), Go4ClientsMappingSupport.DEFAULT_COUNTRY))")
    Go4ClientsMessagesRequest.Destination toDestination(Go4ClientsDestinatarioDto destinatario);

    default HttpEntity<Go4ClientsMessagesRequest> toHttpRequest(ProviderAccount account, String message,
                                                                List<Go4ClientsDestinatarioDto> destinations, String priority,
                                                                Map<String, Map<String, String>> landingCustomFields) {
        return new HttpEntity<>(toRequest(message, destinations, priority, landingCustomFields),
                Go4ClientsMappingSupport.headers(account));
    }

    default String url(ProviderAccount account, String campaignId) {
        return Go4ClientsMappingSupport.baseUrl(account) + Go4ClientsCampaignMapper.PATH + "/" + campaignId;
    }

    // ----------------------------------------------------------------- response

    default Go4ClientsMessagesResponse parseResponse(String body) {
        return Go4ClientsMappingSupport.parse(body, Go4ClientsMessagesResponse.class, Go4ClientsMessagesResponse::new,
                Go4ClientsMessagesResponse::setRawBody);
    }

    @Mapping(target = "success", expression = "java(Go4ClientsMappingSupport.is2xx(httpStatus))")
    @Mapping(target = "campaignId", source = "campaignId")
    @Mapping(target = "reference", ignore = true)
    @Mapping(target = "generatedMessages", expression = "java(response.getGeneratedIds() == null ? 0 : response.getGeneratedIds().size())")
    @Mapping(target = "cuenta", source = "account.code")
    @Mapping(target = "httpStatus", source = "httpStatus")
    @Mapping(target = "errorCode", expression = "java(Go4ClientsMappingSupport.is2xx(httpStatus) ? null : \"HTTP_ERROR\")")
    @Mapping(target = "errorMessage", expression = "java(Go4ClientsMappingSupport.is2xx(httpStatus) ? null : Go4ClientsMappingSupport.truncate(response.getRawBody()))")
    Go4ClientsCampaignResult toResult(Go4ClientsMessagesResponse response, int httpStatus, ProviderAccount account, String campaignId);
}
