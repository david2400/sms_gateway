package com.essenza.sms_gateway.modules.SmsCore.application.services;

import com.essenza.sms_gateway.modules.SmsCore.domain.dto.SendWhatsAppRequest;
import com.essenza.sms_gateway.modules.SmsCore.domain.exception.SmsContenidoException;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchChannel;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchResult;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.DispatchStatus;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.MessageType;
import com.essenza.sms_gateway.modules.SmsCore.domain.model.SmsDispatch;
import com.essenza.sms_gateway.modules.SmsCore.domain.services.SmsDispatchService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WhatsAppSendServiceTest {

    static class Capture extends SmsDispatchService {
        SmsDispatch message;
        DispatchChannel channel;
        String provider;

        Capture() {
            super(null, null, List.of());
        }

        @Override
        public DispatchResult dispatch(SmsDispatch m, DispatchChannel c, String p) {
            message = m;
            channel = c;
            provider = p;
            return DispatchResult.builder().status(DispatchStatus.ENVIADO).build();
        }
    }

    private final Capture engine = new Capture();
    private final WhatsAppSendService service = new WhatsAppSendService(engine);

    private static SendWhatsAppRequest req() {
        return SendWhatsAppRequest.builder().to("3157132338").templateId("ID_PLANTILLA")
                .parameters(List.of("img", "Miguel")).proveedor("INALAMBRIA").datosAdicionales(Map.of("a", "b")).build();
    }

    @Test
    void construyeElMensajeDeWhatsApp() {
        service.send(req(), "k1", null);
        assertEquals(MessageType.WHATSAPP, engine.message.getType());
        assertEquals("ID_PLANTILLA", engine.message.getExternalTemplateId());
        assertEquals(List.of("img", "Miguel"), engine.message.getTemplateParameters());
        assertEquals("[WhatsApp ID_PLANTILLA] img | Miguel", engine.message.getText());
        assertEquals("INALAMBRIA", engine.message.getPreferredProvider());
        assertEquals(DispatchChannel.CENTRAL, engine.channel);
        assertEquals("b", engine.message.getAdditionalData().get("a"));
    }

    @Test
    void endpointDeProveedorNoUsaPreferido() {
        service.send(req(), null, "INALAMBRIA");
        assertEquals(DispatchChannel.PROVEEDOR, engine.channel);
        assertNull(engine.message.getPreferredProvider());
    }

    @Test
    void rechazaTiposDeCanalSms() {
        SendWhatsAppRequest r = req();
        r.setTipoMensaje(MessageType.OTP);
        assertThrows(SmsContenidoException.class, () -> service.send(r, null, null));
    }
}
