package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaMoMessageRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaMoMessageResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InalambriaSmsBidireccionalMapperTest {

    private final InalambriaSmsBidireccionalMapper mapper = Mappers.getMapper(InalambriaSmsBidireccionalMapper.class);

    private static SmsDispatch msg() {
        return SmsDispatch.builder().to("3157132338").type(MessageType.BIDIRECCIONAL)
                .text("Responde 1 para Si, 2 para No").build();
    }

    @Test
    void requestSegunLaDocumentacion() throws Exception {
        ProviderAccount account = ProviderAccount.builder().code("INALAMBRIA_BIDIRECCIONAL").sender("12345").build();
        InalambriaMoMessageRequest r = mapper.toRequest(msg(), account);
        assertEquals("Responde 1 para Si, 2 para No", r.getMessageText());
        assertEquals("573157132338", r.getPhoneNumber());
        assertEquals("12345", r.getShortCode());
        assertNull(r.getUrl());
        assertEquals("{\"MessageText\":\"Responde 1 para Si, 2 para No\",\"PhoneNumber\":\"573157132338\",\"ShortCode\":\"12345\"}",
                new ObjectMapper().writeValueAsString(r));
        // código corto: request > remitente de la cuenta > parametros_json.shortCode
        assertEquals("99999", mapper.shortCode(msg().toBuilder().sender("99999").build(), account));
        assertEquals("55555", mapper.shortCode(msg(), ProviderAccount.builder().parameter("shortCode", "55555").build()));
        assertNull(mapper.shortCode(msg(), ProviderAccount.builder().build()));
        assertEquals("https://x.co/largo", mapper.toRequest(msg().toBuilder().providerOption("url", "https://x.co/largo").build(), account).getUrl());
    }

    @Test
    void respuesta() {
        InalambriaMoMessageResponse ok = mapper.parseResponse("{\"Id\":1342880104349217412345678901,\"Message\":\"OK\",\"Status\":true}");
        assertTrue(mapper.toOutcome(ok, 200, 1).isSuccess());
        assertEquals("1342880104349217412345678901", mapper.numeroTransaccion(ok));

        SendOutcome rechazo = mapper.toOutcome(mapper.parseResponse("{\"Id\":0,\"Message\":\"ERROR\",\"Status\":false}"), 200, 1);
        assertFalse(rechazo.isSuccess());
        assertFalse(rechazo.isRetryable());

        assertFalse(mapper.toOutcome(mapper.parseResponse(""), 401, 1).isRetryable());
        SendOutcome caido = mapper.toOutcome(mapper.parseResponse("caido"), 500, 1);
        assertTrue(caido.isRetryable());
        assertEquals("caido", caido.getDetail());
    }
}
