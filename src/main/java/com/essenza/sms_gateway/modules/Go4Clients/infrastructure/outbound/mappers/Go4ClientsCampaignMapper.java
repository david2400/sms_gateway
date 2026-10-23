package com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.api.Go4ClientsCampaignRequest;
import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.api.Go4ClientsCampaignResponse;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.http.HttpEntity;

/**
 * Mapper ÚNICO de "crear campaña" de Go4Clients ({@code POST /campaigns/sms/v1.0}): request y respuesta.
 * Respuesta {@code { id, reference }}; éxito = HTTP 2xx.
 */
@Mapper(componentModel = "spring", imports = Go4ClientsMappingSupport.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface Go4ClientsCampaignMapper {

    String PATH = "/campaigns/sms/v1.0";

    // ------------------------------------------------------------------ request

    @Mapping(target = "name", source = "name")
    @Mapping(target = "sender", expression = "java(Go4ClientsMappingSupport.resolveSender(account, sender))")
    Go4ClientsCampaignRequest toRequest(ProviderAccount account, String name, String sender);

    default HttpEntity<Go4ClientsCampaignRequest> toHttpRequest(ProviderAccount account, String name, String sender) {
        return new HttpEntity<>(toRequest(account, name, sender), Go4ClientsMappingSupport.headers(account));
    }

    default String url(ProviderAccount account) {
        return Go4ClientsMappingSupport.baseUrl(account) + PATH;
    }

    // ----------------------------------------------------------------- response

    default Go4ClientsCampaignResponse parseResponse(String body) {
        return Go4ClientsMappingSupport.parse(body, Go4ClientsCampaignResponse.class, Go4ClientsCampaignResponse::new,
                Go4ClientsCampaignResponse::setRawBody);
    }

    @Mapping(target = "success", expression = "java(Go4ClientsMappingSupport.is2xx(httpStatus))")
    @Mapping(target = "campaignId", source = "response.id")
    @Mapping(target = "reference", source = "response.reference")
    @Mapping(target = "generatedMessages", ignore = true)
    @Mapping(target = "cuenta", source = "account.code")
    @Mapping(target = "httpStatus", source = "httpStatus")
    @Mapping(target = "errorCode", expression = "java(Go4ClientsMappingSupport.is2xx(httpStatus) ? null : \"HTTP_ERROR\")")
    @Mapping(target = "errorMessage", expression = "java(Go4ClientsMappingSupport.is2xx(httpStatus) ? null : Go4ClientsMappingSupport.truncate(response.getRawBody()))")
    Go4ClientsCampaignResult toResult(Go4ClientsCampaignResponse response, int httpStatus, ProviderAccount account);
}
