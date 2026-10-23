package com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.api.Go4ClientsReportResponse;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignReport;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.http.HttpEntity;

/**
 * Mapper ÚNICO del reporte de campaña de Go4Clients ({@code GET /analytics/sms/v1.0?uniqueCampaignID=...}):
 * request (solo headers) y respuesta {@code { data: { totalMessages, deliveredMessages, failedMessages } }}.
 * Pendientes = total - entregados - fallidos.
 */
@Mapper(componentModel = "spring", imports = Go4ClientsMappingSupport.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface Go4ClientsReportMapper {

    String PATH = "/analytics/sms/v1.0";

    // ------------------------------------------------------------------ request

    default HttpEntity<Void> toHttpRequest(ProviderAccount account) {
        return new HttpEntity<>(Go4ClientsMappingSupport.headers(account));
    }

    default String url(ProviderAccount account, String campaignId) {
        return Go4ClientsMappingSupport.baseUrl(account) + PATH + "?uniqueCampaignID=" + campaignId;
    }

    // ----------------------------------------------------------------- response

    default Go4ClientsReportResponse parseResponse(String body) {
        return Go4ClientsMappingSupport.parse(body, Go4ClientsReportResponse.class, Go4ClientsReportResponse::new,
                Go4ClientsReportResponse::setRawBody);
    }

    @Mapping(target = "success", expression = "java(Go4ClientsMappingSupport.is2xx(httpStatus))")
    @Mapping(target = "campaignId", source = "campaignId")
    @Mapping(target = "total", expression = "java(total(response))")
    @Mapping(target = "delivered", expression = "java(delivered(response))")
    @Mapping(target = "failed", expression = "java(failed(response))")
    @Mapping(target = "pending", expression = "java(Math.max(0, total(response) - delivered(response) - failed(response)))")
    @Mapping(target = "httpStatus", source = "httpStatus")
    @Mapping(target = "errorMessage", expression = "java(Go4ClientsMappingSupport.is2xx(httpStatus) ? null : Go4ClientsMappingSupport.truncate(response.getRawBody()))")
    Go4ClientsCampaignReport toReport(Go4ClientsReportResponse response, int httpStatus, String campaignId);

    default int total(Go4ClientsReportResponse r) {
        return r.getData() == null || r.getData().getTotalMessages() == null ? 0 : r.getData().getTotalMessages();
    }

    default int delivered(Go4ClientsReportResponse r) {
        return r.getData() == null || r.getData().getDeliveredMessages() == null ? 0 : r.getData().getDeliveredMessages();
    }

    default int failed(Go4ClientsReportResponse r) {
        return r.getData() == null || r.getData().getFailedMessages() == null ? 0 : r.getData().getFailedMessages();
    }
}
