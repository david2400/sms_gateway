package com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.mappers;

import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.Go4ClientsDestinatarioDto;
import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.api.Go4ClientsCampaignRequest;
import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.api.Go4ClientsMessagesRequest;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignReport;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Go4ClientsMappersTest {

    private final Go4ClientsCampaignMapper campaign = Mappers.getMapper(Go4ClientsCampaignMapper.class);
    private final Go4ClientsMessageMapper messages = Mappers.getMapper(Go4ClientsMessageMapper.class);
    private final Go4ClientsReportMapper report = Mappers.getMapper(Go4ClientsReportMapper.class);

    private final ProviderAccount account = ProviderAccount.builder().code("GO4CLIENTS_PRINCIPAL")
            .baseUrl("https://x/api/").username("key").password("secret").build();

    @Test
    void crearCampana() {
        Go4ClientsCampaignRequest r = campaign.toRequest(account, "SMS-1", null);
        assertEquals("SMS-1", r.getName());
        assertEquals("RANDOM", r.getSender());
        assertEquals("Comfe", campaign.toRequest(account, "x", "Comfe").getSender());
        assertEquals("https://x/api/campaigns/sms/v1.0", campaign.url(account));
        HttpEntity<Go4ClientsCampaignRequest> http = campaign.toHttpRequest(account, "x", null);
        assertEquals("key", http.getHeaders().getFirst("apiKey"));
        assertEquals("secret", http.getHeaders().getFirst("apiSecret"));

        Go4ClientsCampaignResult ok = campaign.toResult(campaign.parseResponse("{\"id\":\"C1\",\"reference\":\"R9\"}"), 201, account);
        assertTrue(ok.isSuccess());
        assertEquals("C1", ok.getCampaignId());
        assertEquals("R9", ok.getReference());
        assertEquals("GO4CLIENTS_PRINCIPAL", ok.getCuenta());
        assertNull(ok.getErrorCode());

        Go4ClientsCampaignResult fail = campaign.toResult(campaign.parseResponse("unauthorized"), 401, account);
        assertFalse(fail.isSuccess());
        assertEquals("HTTP_ERROR", fail.getErrorCode());
        assertEquals("unauthorized", fail.getErrorMessage());
    }

    @Test
    void enviarMensajesUsaClavesNativas() throws Exception {
        Go4ClientsMessagesRequest r = messages.toRequest("hola",
                List.of(new Go4ClientsDestinatarioDto("+573157132338", null), new Go4ClientsDestinatarioDto("3001112233", "1")),
                null, Map.of());
        assertEquals("HIGH", r.getPriority());
        assertEquals("3157132338", r.getDestinationsList().get(0).getPhoneNumber());
        assertEquals("57", r.getDestinationsList().get(0).getCountryCode());
        assertEquals("1", r.getDestinationsList().get(1).getCountryCode());
        assertNull(r.getLandingCustomFields());
        String json = new ObjectMapper().writeValueAsString(r);
        assertTrue(json.contains("\"destinationsList\":[{\"phoneNumber\":\"3157132338\",\"countryCode\":\"57\"}"), json);
        assertFalse(json.contains("landingCustomFields"), json);
        assertEquals("https://x/api/campaigns/sms/v1.0/C1", messages.url(account, "C1"));

        Go4ClientsCampaignResult ok = messages.toResult(messages.parseResponse("{\"generatedIds\":[\"a\",\"b\"]}"), 201, account, "C1");
        assertTrue(ok.isSuccess());
        assertEquals(2, ok.getGeneratedMessages());
        assertEquals("C1", ok.getCampaignId());
        assertEquals(0, messages.toResult(messages.parseResponse(""), 200, account, "C1").getGeneratedMessages());
    }

    @Test
    void reporte() {
        assertEquals("https://x/api/analytics/sms/v1.0?uniqueCampaignID=C1", report.url(account, "C1"));
        Go4ClientsCampaignReport r = report.toReport(report.parseResponse(
                "{\"data\":{\"totalMessages\":10,\"deliveredMessages\":\"6\",\"failedMessages\":1}}"), 200, "C1");
        assertTrue(r.isSuccess());
        assertEquals(10, r.getTotal());
        assertEquals(6, r.getDelivered());
        assertEquals(1, r.getFailed());
        assertEquals(3, r.getPending());

        Go4ClientsCampaignReport fail = report.toReport(report.parseResponse("error"), 500, "C1");
        assertFalse(fail.isSuccess());
        assertEquals(0, fail.getTotal());
        assertEquals("error", fail.getErrorMessage());
    }
}
