package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InalambriaDispatchPayloadMapperTest {

    private final InalambriaDispatchPayloadMapper mapper = new InalambriaDispatchPayloadMapper();

    @Test
    void normalizaNumeros() {
        assertEquals("573157132338", mapper.normalizePhone("3157132338", "57"));
        assertEquals("573157132338", mapper.normalizePhone("+573157132338", "57"));
        assertEquals("573157132338", mapper.normalizePhone("573157132338", "57"));
    }

    @Test
    void requestSobrescribeParametrosDeLaCuenta() {
        ProviderAccount cuenta = ProviderAccount.builder().code("INALAMBRIA_TEMPLATE")
                .parameters(Map.of("messagePattern", "1", "templateId", "5", "type", "1")).build();
        SmsDispatch msg = SmsDispatch.builder().to("3157132338").text("hola").type(MessageType.TEMPLATE)
                .providerOption("templateId", "9").build();

        Map<String, Object> payload = mapper.toPayload(msg, cuenta);

        assertEquals("1", payload.get("MessagePattern"));
        assertEquals(9, payload.get("TemplateId"));
        assertEquals(1, payload.get("Type"));
        assertEquals("573157132338", payload.get("Devices"));
        assertEquals(28, payload.get("TransactionNumber").toString().length());
        assertNotEquals(mapper.toPayload(msg, cuenta).get("TransactionNumber"), payload.get("TransactionNumber"));
    }
}
