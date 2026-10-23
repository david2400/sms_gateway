package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaMasivoRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InalambriaSmsMasivoMapperTest {

    private final InalambriaSmsMasivoMapper mapper = Mappers.getMapper(InalambriaSmsMasivoMapper.class);

    @Test
    void agrupaPorTextoYNormalizaNumeros() throws Exception {
        SmsDispatch a = msg("3157132338", "Promo");
        SmsDispatch b = msg("+573001112233", "Promo");
        SmsDispatch c = msg("3009998877", "Hola Ana");
        ProviderAccount account = ProviderAccount.builder().code("INALAMBRIA_MASIVO").username("u").password("p")
                .parameter("notifyUrl", "https://mi.api/callback").parameter("xAuth", "tok").build();

        InalambriaMasivoRequest r = mapper.toRequest(List.of(a, b, c), account, "L1-1");

        assertEquals("L1-1", r.getBulkId());
        assertEquals(2, r.getMessages().size());
        assertEquals("Promo", r.getMessages().get(0).getText());
        assertEquals("573157132338", r.getMessages().get(0).getDestinations().get(0).getTo());
        assertEquals("573001112233", r.getMessages().get(0).getDestinations().get(1).getTo());
        assertEquals(a.getId().toString(), r.getMessages().get(0).getDestinations().get(0).getMessageId());
        assertEquals("https://mi.api/callback", r.getMessages().get(0).getNotifyUrl());
        assertEquals("application/json", r.getMessages().get(0).getNotifyContentType());
        assertEquals(false, r.getMessages().get(1).getFlash());

        HttpEntity<InalambriaMasivoRequest> http = mapper.toHttpRequest(List.of(a), account, "L");
        assertEquals("tok", http.getHeaders().getFirst("x-auth"));
        // sin notifyUrl no se envían esos campos
        String json = new ObjectMapper().writeValueAsString(mapper.toRequest(List.of(a), ProviderAccount.builder().build(), null));
        assertFalse(json.contains("notify") || json.contains("bulkId"), json);
    }

    @Test
    void respuestaInterpretaStatus() {
        assertTrue(mapper.toOutcome(mapper.parseResponse("{\"TransactionNumber\":\"123\",\"Status\":0}"), 200, 1).isSuccess());

        SendOutcome rechazo = mapper.toOutcome(mapper.parseResponse("{\"status\":\"1\",\"messageText\":\"Numero invalido\"}"), 200, 1);
        assertFalse(rechazo.isSuccess());
        assertFalse(rechazo.isRetryable());
        assertEquals("Inalambria rechazó el lote (Status 1): Numero invalido", rechazo.getDetail());

        assertTrue(mapper.toOutcome(mapper.parseResponse("OK"), 200, 1).isSuccess());
        assertFalse(mapper.toOutcome(mapper.parseResponse(""), 401, 1).isRetryable());
        SendOutcome caido = mapper.toOutcome(mapper.parseResponse("caido"), 503, 1);
        assertTrue(caido.isRetryable());
        assertEquals("caido", caido.getDetail());
        assertNull(mapper.toOutcome(mapper.parseResponse(null), 200, 1).getDetail());
    }

    private static SmsDispatch msg(String to, String text) {
        return SmsDispatch.builder().to(to).text(text).type(MessageType.MASIVO).build();
    }
}
