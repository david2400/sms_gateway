package com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.providers;

import com.essenza.sms_gateway.modules.Go4Clients.domain.dto.Go4ClientsDestinatarioDto;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignReport;
import com.essenza.sms_gateway.modules.Go4Clients.domain.model.Go4ClientsCampaignResult;
import com.essenza.sms_gateway.modules.Go4Clients.domain.ports.Go4ClientsCampaignPort;
import com.essenza.sms_gateway.modules.Go4Clients.infrastructure.outbound.mappers.Go4ClientsPayloadMapper;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Go4ClientsSmsSenderTest {

    private final ProviderAccount account = ProviderAccount.builder().code("GO4CLIENTS_PRINCIPAL")
            .providerCode("GO4CLIENTS").username("key").password("secret").baseUrl("https://x/api").build();
    private final SmsDispatch msg = SmsDispatch.builder().to("3157132338").text("hola").type(MessageType.OTP).build();

    @Test
    void creaCampanaYEnvia() {
        FakePort port = new FakePort(true, true);
        SendOutcome out = new Go4ClientsSmsSender(port).send(msg, account);

        assertTrue(out.isSuccess());
        assertEquals("SMS-" + msg.getId(), port.campaignName);
        assertEquals("C1", port.sentCampaign);
        assertEquals("3157132338", port.destinations.get(0).getPhoneNumber());
    }

    @Test
    void fallaCrearCampanaNoEnvia() {
        FakePort port = new FakePort(false, true);
        SendOutcome out = new Go4ClientsSmsSender(port).send(msg, account);

        assertFalse(out.isSuccess());
        assertEquals(401, out.getHttpStatus());
        assertEquals(null, port.sentCampaign);
    }

    @Test
    void payloadUsaClavesNativas() {
        Go4ClientsPayloadMapper mapper = new Go4ClientsPayloadMapper();
        Map<String, Object> p = mapper.sendMessagesPayload("hola",
                List.of(new Go4ClientsDestinatarioDto("+573157132338", null)), null, null);
        @SuppressWarnings("unchecked")
        Map<String, String> d = ((List<Map<String, String>>) p.get("destinationsList")).get(0);
        assertEquals("3157132338", d.get("phoneNumber"));
        assertEquals("57", d.get("countryCode"));
        assertEquals("HIGH", p.get("priority"));
        assertEquals("RANDOM", mapper.resolveSender(account, null));
        assertEquals("https://x/api/campaigns/sms/v1.0", mapper.campaignsUrl(account));
    }

    static class FakePort implements Go4ClientsCampaignPort {
        final boolean createOk;
        final boolean sendOk;
        String campaignName;
        String sentCampaign;
        List<Go4ClientsDestinatarioDto> destinations;

        FakePort(boolean createOk, boolean sendOk) {
            this.createOk = createOk;
            this.sendOk = sendOk;
        }

        @Override
        public Go4ClientsCampaignResult createCampaign(ProviderAccount a, String name, String sender) {
            campaignName = name;
            return createOk
                    ? Go4ClientsCampaignResult.builder().success(true).campaignId("C1").httpStatus(200).build()
                    : Go4ClientsCampaignResult.failure(a.getCode(), 401, "HTTP_ERROR", "unauthorized");
        }

        @Override
        public Go4ClientsCampaignResult sendMessages(ProviderAccount a, String campaignId, String message,
                                                     List<Go4ClientsDestinatarioDto> d, String priority,
                                                     Map<String, Map<String, String>> landing) {
            sentCampaign = campaignId;
            destinations = d;
            return sendOk
                    ? Go4ClientsCampaignResult.builder().success(true).campaignId(campaignId).httpStatus(201).build()
                    : Go4ClientsCampaignResult.failure(a.getCode(), 500, "HTTP_ERROR", "error");
        }

        @Override
        public Go4ClientsCampaignReport getReport(ProviderAccount a, String campaignId) {
            return Go4ClientsCampaignReport.builder().campaignId(campaignId).build();
        }
    }
}
