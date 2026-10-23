package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaMtMessageRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaRestResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InalambriaSmsMapperTest {

    private final InalambriaSmsMapper mapper = Mappers.getMapper(InalambriaSmsMapper.class);

    @Test
    void normalizaNumeros() {
        assertEquals("573157132338", InalambriaMappingSupport.normalizePhone("3157132338", "57"));
        assertEquals("573157132338", InalambriaMappingSupport.normalizePhone("+573157132338", "57"));
        assertEquals("573157132338", InalambriaMappingSupport.normalizePhone("573157132338", "57"));
    }

    @Test
    void requestSobrescribeParametrosDeLaCuenta() throws Exception {
        ProviderAccount cuenta = ProviderAccount.builder().code("INALAMBRIA_TEMPLATE").username("u").password("p")
                .parameters(Map.of("messagePattern", "1", "templateId", "5", "type", "1")).build();
        SmsDispatch msg = SmsDispatch.builder().to("3157132338").text("hola").type(MessageType.TEMPLATE)
                .providerOption("templateId", "9").build();

        InalambriaMtMessageRequest r = mapper.toRequest(msg, cuenta);

        assertEquals("1", r.getMessagePattern());
        assertEquals(9, r.getTemplateId());
        assertEquals(1, r.getType());
        assertEquals("573157132338", r.getDevices());
        assertEquals(0, r.getFlashSms());
        assertEquals("", r.getUrl());
        assertEquals(28, r.getTransactionNumber().length());
        assertNotEquals(mapper.toRequest(msg, cuenta).getTransactionNumber(), r.getTransactionNumber());

        // JSON con los nombres exactos de la API
        String json = new ObjectMapper().writeValueAsString(r);
        assertTrue(json.startsWith("{\"Type\":1,\"MessageText\":\"hola\",\"Devices\":\"573157132338\""), json);
        assertTrue(json.contains("\"FlashSMS\":0"), json);

        HttpEntity<InalambriaMtMessageRequest> http = mapper.toHttpRequest(msg, cuenta);
        assertTrue(http.getHeaders().getFirst("Authorization").startsWith("Basic "));
    }

    @Test
    void respuestaExitoSoloPor2xx() {
        SendOutcome ok = mapper.toOutcome(mapper.parseResponse("{\"Status\":0}"), 200, 15);
        assertTrue(ok.isSuccess());
        assertNull(ok.getDetail());
        assertEquals(15, ok.getDurationMs());

        InalambriaRestResponse error = mapper.parseResponse("<html>error</html>");
        SendOutcome fail = mapper.toOutcome(error, 503, 3);
        assertFalse(fail.isSuccess());
        assertTrue(fail.isRetryable());
        assertEquals(503, fail.getHttpStatus());
        assertEquals("<html>error</html>", fail.getDetail());
    }
}
