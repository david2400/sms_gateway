package com.essenza.sms_gateway.modules.INALAMBRIA.infrastructure.outbound.mappers.inalambria;

import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaWhatsAppRequest;
import com.essenza.sms_gateway.modules.INALAMBRIA.domain.dto.api.InalambriaWhatsAppResponse;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.ProviderAccount;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SendOutcome;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InalambriaWhatsAppMapperTest {

    private final InalambriaWhatsAppMapper mapper = Mappers.getMapper(InalambriaWhatsAppMapper.class);

    private static SmsDispatch wa() {
        return SmsDispatch.builder().to("3157132338").type(MessageType.WHATSAPP).externalTemplateId("ID_PLANTILLA")
                .templateParameter("https://example.com/img.png").templateParameter("Miguel")
                .additionalDataEntry("campana", "nov").build();
    }

    @Test
    void requestSegunLaDocumentacion() {
        ProviderAccount account = ProviderAccount.builder().code("INALAMBRIA_WHATSAPP").sender("3000000000").build();
        InalambriaWhatsAppRequest r = mapper.toRequest(wa(), account);
        assertEquals("573000000000", r.getSource());
        assertEquals("573157132338", r.getDestination());
        assertEquals("ID_PLANTILLA", r.getTemplateId());
        assertEquals(List.of("https://example.com/img.png", "Miguel"), r.getParameters());
        assertEquals(Map.of("campana", "nov"), r.getOptionalAditionalData());
        // source: request > remitente de la cuenta > parametros_json.source
        assertEquals("573111111111", mapper.source(wa().toBuilder().sender("573111111111").build(), account));
        assertEquals("573222222222", mapper.source(wa(), ProviderAccount.builder().parameter("source", "573222222222").build()));
        // sin parámetros ni datos adicionales: listas/mapas vacíos (la API los exige)
        InalambriaWhatsAppRequest vacio = mapper.toRequest(SmsDispatch.builder().to("3157132338").externalTemplateId("T").build(), account);
        assertEquals(List.of(), vacio.getParameters());
        assertEquals(Map.of(), vacio.getOptionalAditionalData());
    }

    @Test
    void respuestaSinPerderElNumeroDeTransaccion() {
        InalambriaWhatsAppResponse ok = mapper.parseResponse(
                "{\"messageText\":\"Recepción exitosa\",\"httpStatusCode\":200,\"transactionNumber\":1342880104349217412345678901,\"errors\":null,\"errorDetails\":null}");
        assertTrue(mapper.toOutcome(ok, 200, 1).isSuccess());
        assertEquals("1342880104349217412345678901", mapper.numeroTransaccion(ok));

        SendOutcome bad = mapper.toOutcome(mapper.parseResponse(
                "{\"messageText\":\"Error\",\"httpStatusCode\":400,\"transactionNumber\":0,\"errors\":\"Parámetros vacíos | Número inválido\"}"), 400, 1);
        assertFalse(bad.isSuccess());
        assertFalse(bad.isRetryable());
        assertEquals("Error: Parámetros vacíos | Número inválido", bad.getDetail());

        // 200 HTTP con error en el body: manda el código del body
        SendOutcome errorEnBody = mapper.toOutcome(mapper.parseResponse("{\"httpStatusCode\":400,\"transactionNumber\":0}"), 200, 1);
        assertFalse(errorEnBody.isSuccess());
        assertEquals(400, errorEnBody.getHttpStatus());
        assertFalse(errorEnBody.isRetryable());

        assertTrue(mapper.toOutcome(mapper.parseResponse("{\"messageText\":\"x\",\"httpStatusCode\":500,\"transactionNumber\":0}"), 500, 1).isRetryable());
    }
}
